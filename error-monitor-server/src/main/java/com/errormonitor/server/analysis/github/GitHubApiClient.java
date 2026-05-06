package com.errormonitor.server.analysis.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final int MAX_FILE_LENGTH = 3000;

    private final GitHubProperties properties;
    private final HttpClient httpClient;

    public GitHubApiClient(GitHubProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getFileContent(String repo, String filePath, String branch) {
        try {
            String ref = (branch != null && !branch.isBlank()) ? branch : "main";
            String url = String.format("https://api.github.com/repos/%s/contents/%s?ref=%s",
                    repo, filePath, ref);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/vnd.github.v3.raw")
                    .header("User-Agent", "ErrorMonitor")
                    .timeout(Duration.ofSeconds(15))
                    .GET();

            if (properties.getToken() != null && !properties.getToken().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + properties.getToken());
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String content = response.body();
                log.info("[GitHub] 파일 조회 성공 - repo: {}, path: {}, size: {}자", repo, filePath, content.length());
                if (content.length() > MAX_FILE_LENGTH) {
                    return content.substring(0, MAX_FILE_LENGTH) + "\n... (truncated)";
                }
                return content;
            } else if (response.statusCode() == 404) {
                log.warn("[GitHub] 파일 없음 - repo: {}, path: {}", repo, filePath);
                return "File not found: " + filePath;
            } else {
                log.warn("[GitHub] 조회 실패 - repo: {}, path: {}, status: {}", repo, filePath, response.statusCode());
                return "Error fetching file (HTTP " + response.statusCode() + "): " + filePath;
            }
        } catch (Exception e) {
            log.error("GitHub API 호출 실패 - repo: {}, path: {}", repo, filePath, e);
            return "Error: Failed to fetch file - " + e.getMessage();
        }
    }
}
