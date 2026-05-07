package com.errormonitor.server.analysis.groq;

import com.errormonitor.server.analysis.github.GitHubApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class GroqApiClient {

    private static final Logger log = LoggerFactory.getLogger(GroqApiClient.class);
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int MAX_RETRIES = 3;

    private final GroqProperties properties;
    private final GitHubApiClient gitHubApiClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String systemPrompt;

    public GroqApiClient(GroqProperties properties,
                         GitHubApiClient gitHubApiClient,
                         ObjectMapper objectMapper) throws IOException {
        this.properties = properties;
        this.gitHubApiClient = gitHubApiClient;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.systemPrompt = new ClassPathResource("prompts/error-analysis-system.txt")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    public String analyze(String githubRepo, String exceptionType, String message,
                          String stackTrace, String sourceClass) {
        try {
            ArrayNode messages = objectMapper.createArrayNode();

            // System message
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", buildSystemPrompt());
            messages.add(systemMsg);

            // User message
            String userMessage = buildUserMessage(exceptionType, message, stackTrace, sourceClass, githubRepo);
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            for (int round = 0; round < properties.getMaxToolRounds(); round++) {
                JsonNode response = callGroq(messages);
                JsonNode choice = response.path("choices").path(0);
                JsonNode responseMessage = choice.path("message");
                String finishReason = choice.path("finish_reason").asText();

                // Add assistant message to conversation
                messages.add(responseMessage.deepCopy());

                if ("stop".equals(finishReason)) {
                    return responseMessage.path("content").asText();
                }

                if ("tool_calls".equals(finishReason)) {
                    JsonNode toolCalls = responseMessage.path("tool_calls");
                    for (JsonNode toolCall : toolCalls) {
                        String toolCallId = toolCall.path("id").asText();
                        String functionName = toolCall.path("function").path("name").asText();
                        JsonNode arguments = objectMapper.readTree(toolCall.path("function").path("arguments").asText());

                        if ("get_file_content".equals(functionName)) {
                            String filePath = arguments.path("file_path").asText();
                            String branch = arguments.has("branch") ? arguments.path("branch").asText() : "main";

                            String fileContent = gitHubApiClient.getFileContent(githubRepo, filePath, branch);

                            ObjectNode toolResultMsg = objectMapper.createObjectNode();
                            toolResultMsg.put("role", "tool");
                            toolResultMsg.put("tool_call_id", toolCallId);
                            toolResultMsg.put("content", fileContent);
                            messages.add(toolResultMsg);
                        }
                    }
                } else {
                    return responseMessage.path("content").asText("");
                }
            }

            return "{\"rootCause\": \"분석 라운드 초과\", \"solutions\": []}";
        } catch (Exception e) {
            log.error("Groq API 분석 실패", e);
            throw new RuntimeException("Groq API 호출 실패: " + e.getMessage(), e);
        }
    }

    private JsonNode callGroq(ArrayNode messages) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", properties.getModel());
        requestBody.put("max_tokens", properties.getMaxTokens());
        requestBody.set("messages", messages);
        requestBody.set("tools", buildTools());

        String body = objectMapper.writeValueAsString(requestBody);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }

            if (response.statusCode() == 429 && attempt < MAX_RETRIES) {
                long waitSeconds = parseRetryAfter(response.body());
                log.warn("Groq API 요청 제한(429) - {}초 후 재시도 ({}/{})", waitSeconds, attempt + 1, MAX_RETRIES);
                Thread.sleep(waitSeconds * 1000);
                continue;
            }

            throw new RuntimeException("Groq API 응답 오류 (HTTP " + response.statusCode() + "): " + response.body());
        }

        throw new RuntimeException("Groq API 재시도 횟수 초과");
    }

    private long parseRetryAfter(String responseBody) {
        try {
            JsonNode error = objectMapper.readTree(responseBody);
            String message = error.path("error").path("message").asText();
            // "Please try again in 17.44s" 패턴에서 초 추출
            int idx = message.indexOf("try again in ");
            if (idx != -1) {
                String afterPrefix = message.substring(idx + "try again in ".length());
                String seconds = afterPrefix.replaceAll("[^0-9.]", "").split("\\.")[0];
                return Long.parseLong(seconds) + 1; // 올림하여 여유 확보
            }
        } catch (Exception ignored) {
        }
        return 20; // 파싱 실패 시 기본 20초 대기
    }

    private String buildSystemPrompt() {
        return systemPrompt;
    }

    private ArrayNode buildTools() {
        ArrayNode tools = objectMapper.createArrayNode();
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");

        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", "get_file_content");
        function.put("description", "GitHub 저장소에서 파일 내용을 조회합니다. 에러가 발생한 소스코드를 확인하는데 사용하세요.");

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");

        ObjectNode props = objectMapper.createObjectNode();

        ObjectNode filePath = objectMapper.createObjectNode();
        filePath.put("type", "string");
        filePath.put("description", "저장소 루트 기준 파일 경로 (예: src/main/java/com/example/Service.java)");
        props.set("file_path", filePath);

        ObjectNode branch = objectMapper.createObjectNode();
        branch.put("type", "string");
        branch.put("description", "브랜치명 (기본값: main)");
        props.set("branch", branch);

        parameters.set("properties", props);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("file_path");
        parameters.set("required", required);

        function.set("parameters", parameters);
        tool.set("function", function);
        tools.add(tool);

        return tools;
    }

    private String buildUserMessage(String exceptionType, String message,
                                    String stackTrace, String sourceClass, String githubRepo) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 에러를 분석해주세요.\n\n");
        sb.append("## 에러 정보\n");
        sb.append("- **예외 타입**: ").append(exceptionType).append("\n");
        sb.append("- **메시지**: ").append(message != null ? message : "없음").append("\n");
        sb.append("- **GitHub 저장소**: ").append(githubRepo).append("\n");

        if (sourceClass != null && !sourceClass.isBlank()) {
            sb.append("- **발생 클래스**: ").append(sourceClass).append("\n");
            String filePath = sourceClass.replace(".", "/") + ".java";
            sb.append("- **예상 파일 경로**: src/main/java/").append(filePath).append("\n");
        }

        if (stackTrace != null && !stackTrace.isBlank()) {
            sb.append("\n## 스택 트레이스\n```\n").append(stackTrace).append("\n```\n");
        }

        sb.append("\nIMPORTANT: You MUST use the get_file_content tool to read the actual source code files from the GitHub repository before providing your analysis.");
        sb.append("\nStart by reading the file where the error occurred, then read related files as needed.");
        if (sourceClass != null && !sourceClass.isBlank()) {
            String suggestedPath = "src/main/java/" + sourceClass.replace(".", "/") + ".java";
            sb.append("\nFirst, call get_file_content with file_path: \"").append(suggestedPath).append("\"");
        }
        return sb.toString();
    }
}
