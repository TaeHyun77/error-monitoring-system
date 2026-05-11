package com.errormonitor.sdk.transport;

import com.errormonitor.sdk.event.ErrorEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class ErrorSendTask implements Runnable {
    private final HttpErrorTransport transport;
    private final ErrorEvent event;

    @Override
    public void run() {
        try {
            transport.send(event);
        } catch (Exception e) {
            log.debug("에러 이벤트 전송 실패", e);
        }
    }
}