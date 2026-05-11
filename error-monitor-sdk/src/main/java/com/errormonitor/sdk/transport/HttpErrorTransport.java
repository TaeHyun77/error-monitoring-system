package com.errormonitor.sdk.transport;

import com.errormonitor.sdk.event.ErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
public class HttpErrorTransport {
    private final RestTemplate restTemplate;
    private final String serverUrl;
    private final String apiKey;
    private final FileBackupTransport fileBackupTransport;

    // 에러 정보를 에러 서버로 전달, 실패 시 파일 백업
    public void send(ErrorEvent event) {
        SendResult result = trySend(event);

        if (result != SendResult.SUCCESS) {
            log.warn("에러 이벤트 전송 실패, 백업 저장 진행");
            fileBackupTransport.backup(event);
        }
    }

    public SendResult trySend(ErrorEvent event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);

            HttpEntity<ErrorEvent> request = new HttpEntity<>(event, headers);
            restTemplate.postForEntity(serverUrl + "/api/errors", request, Void.class);
            log.debug("에러 이벤트 전송 성공: {}", event.getFingerprint());
            return SendResult.SUCCESS;
        } catch (HttpClientErrorException e) {
            log.warn("에러 이벤트 서버 거부 ({}): {}", e.getStatusCode(), e.getMessage());
            return SendResult.CLIENT_ERROR;
        } catch (Exception e) {
            log.debug("에러 이벤트 전송 실패: {}", e.getMessage());
            return SendResult.SERVER_ERROR;
        }
    }
}
