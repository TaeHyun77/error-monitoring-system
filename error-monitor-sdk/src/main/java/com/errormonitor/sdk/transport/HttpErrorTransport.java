package com.errormonitor.sdk.transport;

import com.errormonitor.sdk.model.ErrorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class HttpErrorTransport {

    private final RestTemplate restTemplate;
    private final String serverUrl;
    private final String apiKey;
    private final FileBackupTransport fileBackupTransport;

    public HttpErrorTransport(RestTemplate restTemplate, String serverUrl, String apiKey,
                              FileBackupTransport fileBackupTransport) {
        this.restTemplate = restTemplate;
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
        this.fileBackupTransport = fileBackupTransport;
    }

    public void send(ErrorEvent event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);

            HttpEntity<ErrorEvent> request = new HttpEntity<>(event, headers);
            restTemplate.postForEntity(serverUrl + "/api/errors", request, Void.class);
            log.debug("에러 이벤트 전송 성공: {}", event.getFingerprint());
        } catch (Exception e) {
            log.warn("에러 이벤트 전송 실패, 백업 저장 진행: {}", e.getMessage());
            fileBackupTransport.backup(event);
        }
    }
}
