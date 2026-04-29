package com.errormonitor.server.analysis;

import com.errormonitor.server.analysis.groq.GroqApiClient;
import com.errormonitor.server.analysis.repository.ErrorAnalysisRepository;
import com.errormonitor.server.error.event.ErrorEvent;
import com.errormonitor.server.error.event.repository.ErrorEventRepository;
import com.errormonitor.server.error.group.ErrorGroup;
import com.errormonitor.server.error.group.repository.ErrorGroupRepository;
import com.errormonitor.server.project.Project;
import com.errormonitor.server.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class ErrorAnalysisExecutor {

    private static final Logger log = LoggerFactory.getLogger(ErrorAnalysisExecutor.class);

    private static final int SHORT_THRESHOLD = 30;
    private static final int MAX_FRAMES_PER_BLOCK = 5;

    private static final List<String> FRAMEWORK_PREFIXES = List.of(
            "at org.springframework.",
            "at java.",
            "at javax.",
            "at jakarta.",
            "at sun.",
            "at jdk.",
            "at com.fasterxml.",
            "at org.hibernate.",
            "at org.apache.",
            "at org.thymeleaf.",
            "at com.zaxxer.",
            "at org.postgresql.",
            "at io.netty.",
            "at reactor."
    );

    private final ErrorAnalysisRepository analysisRepository;
    private final ErrorGroupRepository errorGroupRepository;
    private final ErrorEventRepository errorEventRepository;
    private final ProjectRepository projectRepository;
    private final GroqApiClient groqApiClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ErrorAnalysisExecutor(ErrorAnalysisRepository analysisRepository,
                                 ErrorGroupRepository errorGroupRepository,
                                 ErrorEventRepository errorEventRepository,
                                 ProjectRepository projectRepository,
                                 GroqApiClient groqApiClient,
                                 ObjectMapper objectMapper,
                                 TransactionTemplate transactionTemplate) {
        this.analysisRepository = analysisRepository;
        this.errorGroupRepository = errorGroupRepository;
        this.errorEventRepository = errorEventRepository;
        this.projectRepository = projectRepository;
        this.groqApiClient = groqApiClient;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Async
    public void execute(Long analysisId, Long groupId) {
        try {
            // TX1: status -> IN_PROGRESS
            transactionTemplate.executeWithoutResult(status -> {
                ErrorAnalysis analysis = analysisRepository.findById(analysisId).orElseThrow();
                analysis.startAnalysis();
            });

            // Gather error info
            ErrorGroup group = transactionTemplate.execute(status ->
                    errorGroupRepository.findById(groupId).orElseThrow());

            String githubRepo = transactionTemplate.execute(status ->
                    projectRepository.findByProjectId(group.getProjectId())
                            .map(Project::getGithubRepo)
                            .orElse(null));

            if (githubRepo == null || githubRepo.isBlank()) {
                transactionTemplate.executeWithoutResult(status -> {
                    ErrorAnalysis analysis = analysisRepository.findById(analysisId).orElseThrow();
                    analysis.failAnalysis("GitHub 저장소 정보가 설정되지 않았습니다.");
                });
                return;
            }

            // Fetch stack trace from latest event
            String stackTrace = transactionTemplate.execute(status -> {
                ErrorEvent latestEvent = errorEventRepository.findFirstByErrorGroupIdOrderByCreatedAtDesc(groupId);
                return latestEvent != null ? latestEvent.getStackTrace() : null;
            });

            // 예외 블록 구조를 유지하면서 앱 프레임 우선으로 스택 트레이스 다듬기
            String trimmedStackTrace = trimStackTrace(stackTrace);

            // Call Groq API (outside transaction, may take 30-120s)
            String result = groqApiClient.analyze(
                    githubRepo,
                    group.getExceptionType(),
                    group.getMessage(),
                    trimmedStackTrace,
                    group.getSourceClass()
            );

            // TX2: Save results
            transactionTemplate.executeWithoutResult(status -> {
                ErrorAnalysis analysis = analysisRepository.findById(analysisId).orElseThrow();
                try {
                    JsonNode json = objectMapper.readTree(result);
                    String rootCause = json.path("rootCause").asText();
                    String solutions = objectMapper.writeValueAsString(json.path("solutions"));
                    analysis.completeAnalysis(rootCause, solutions);
                } catch (Exception e) {
                    analysis.completeAnalysis(result, "[]");
                }
            });

            log.info("AI 분석 완료 - analysisId: {}, groupId: {}", analysisId, groupId);

        } catch (Exception e) {
            log.error("AI 분석 실패 - analysisId: {}, groupId: {}", analysisId, groupId, e);
            transactionTemplate.executeWithoutResult(status -> {
                analysisRepository.findById(analysisId).ifPresent(analysis ->
                        analysis.failAnalysis("분석 중 오류 발생: " + e.getMessage()));
            });
        }
    }

    /**
     * 예외 블록 구조를 유지하면서 블록별로 앱 프레임 우선 + 프레임워크 프레임 보조로 선별한다.
     * 짧은 스택 트레이스는 원문 그대로 반환하고, 파싱 실패 시 앞 30줄만 반환한다.
     */
    String trimStackTrace(String rawStackTrace) {
        if (rawStackTrace == null || rawStackTrace.isBlank()) {
            return rawStackTrace;
        }

        String[] lines = rawStackTrace.split("\n");

        // 1단계: 짧은 스택 트레이스 바이패스
        if (lines.length <= SHORT_THRESHOLD) {
            return rawStackTrace;
        }

        try {
            // 2단계: 예외 블록 분리
            List<List<String>> blocks = new ArrayList<>();
            List<String> currentBlock = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (!currentBlock.isEmpty() && trimmed.startsWith("Caused by:")) {
                    blocks.add(currentBlock);
                    currentBlock = new ArrayList<>();
                }
                currentBlock.add(line);
            }
            if (!currentBlock.isEmpty()) {
                blocks.add(currentBlock);
            }

            // 3~5단계: 블록별 프레임 선별 후 재조립
            StringBuilder result = new StringBuilder();
            for (int b = 0; b < blocks.size(); b++) {
                List<String> block = blocks.get(b);
                if (b > 0) {
                    result.append("\n");
                }

                // 헤더 줄(예외 메시지 또는 Caused by 줄) 추출
                String header = block.get(0);
                result.append(header);

                // at 프레임 분류
                List<String> appFrames = new ArrayList<>();
                List<String> otherFrames = new ArrayList<>();
                int totalFrames = 0;

                for (int i = 1; i < block.size(); i++) {
                    String trimmed = block.get(i).trim();
                    if (!trimmed.startsWith("at ")) {
                        continue;
                    }
                    totalFrames++;
                    if (isFrameworkFrame(trimmed)) {
                        otherFrames.add(block.get(i));
                    } else {
                        appFrames.add(block.get(i));
                    }
                }

                // 4단계: 앱 프레임 우선, 남은 슬롯에 프레임워크 프레임 채우기
                List<String> selected = new ArrayList<>();
                int appCount = Math.min(appFrames.size(), MAX_FRAMES_PER_BLOCK);
                for (int i = 0; i < appCount; i++) {
                    selected.add(appFrames.get(i));
                }
                int remaining = MAX_FRAMES_PER_BLOCK - selected.size();
                int otherCount = Math.min(otherFrames.size(), remaining);
                for (int i = 0; i < otherCount; i++) {
                    selected.add(otherFrames.get(i));
                }

                for (String frame : selected) {
                    result.append("\n").append(frame);
                }

                int omitted = totalFrames - selected.size();
                if (omitted > 0) {
                    result.append("\n\t... ").append(omitted).append(" more frames (truncated)");
                }
            }

            return result.toString();

        } catch (Exception e) {
            // 6단계: 파싱 실패 fallback — 앞 30줄만 반환
            log.warn("스택 트레이스 파싱 실패, 앞 {}줄로 대체", SHORT_THRESHOLD, e);
            List<String> fallback = new ArrayList<>();
            for (int i = 0; i < Math.min(lines.length, SHORT_THRESHOLD); i++) {
                fallback.add(lines[i]);
            }
            return String.join("\n", fallback);
        }
    }

    private boolean isFrameworkFrame(String line) {
        for (String prefix : FRAMEWORK_PREFIXES) {
            if (line.contains(prefix)) {
                return true;
            }
        }
        return false;
    }
}
