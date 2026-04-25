package com.errormonitor.server.notification;

import com.errormonitor.server.error.group.ErrorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class SlackNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationService.class);

    private final SlackNotificationProperties properties;
    private final HttpClient httpClient;

    public SlackNotificationService(SlackNotificationProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void send(NotificationEvent event) {
        if (!properties.isEnabled() || properties.getWebhookUrl() == null || properties.getWebhookUrl().isBlank()) {
            log.debug("Slack 알림이 비활성화 상태이거나 Webhook URL이 설정되지 않았습니다.");
            return;
        }

        try {
            String payload = buildPayload(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Slack 알림 전송 실패 - statusCode: {}, body: {}", response.statusCode(), response.body());
            } else {
                log.info("Slack 알림 전송 성공 - type: {}, groupId: {}", event.getType(), event.getErrorGroup().getId());
            }
        } catch (Exception e) {
            log.warn("Slack 알림 전송 중 오류 발생", e);
        }
    }

    private String buildPayload(NotificationEvent event) {
        ErrorGroup group = event.getErrorGroup();
        NotificationEvent.Type type = event.getType();

        String emoji = type == NotificationEvent.Type.NEW_ERROR ? "\uD83D\uDEA8" : "\uD83D\uDD04";
        String typeLabel = type == NotificationEvent.Type.NEW_ERROR ? "신규 에러 발생" : "에러 재발 (REGRESSED)";

        String sourceLocation = formatSourceLocation(group);

        String text = String.format("%s *%s*\n\n"
                        + "*프로젝트:* %s\n"
                        + "*예외 타입:* `%s`\n"
                        + "*메시지:* %s\n"
                        + "*발생 위치:* `%s`\n"
                        + "*발생 횟수:* %d회",
                emoji, typeLabel,
                escapeJson(group.getProjectId()),
                escapeJson(group.getExceptionType()),
                escapeJson(truncate(group.getMessage(), 200)),
                escapeJson(sourceLocation),
                group.getEventCount());

        return "{\"text\": \"" + escapeJson(text) + "\"}";
    }

    private String formatSourceLocation(ErrorGroup group) {
        if (group.getSourceClass() == null) {
            return "알 수 없음";
        }
        String location = group.getSourceClass() + "." + group.getSourceMethod();
        if (group.getSourceLineNumber() != null) {
            location += ":" + group.getSourceLineNumber();
        }
        return location;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "N/A";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
