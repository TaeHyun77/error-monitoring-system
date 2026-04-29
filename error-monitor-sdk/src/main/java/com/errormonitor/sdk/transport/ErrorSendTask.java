package com.errormonitor.sdk.transport;

import com.errormonitor.sdk.model.ErrorEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorSendTask implements Runnable {
    private final HttpErrorTransport transport;
    @Getter
    private final ErrorEvent event;

    public ErrorSendTask(HttpErrorTransport transport, ErrorEvent event) {
        this.transport = transport;
        this.event = event;
    }

    @Override
    public void run() {
        try {
            transport.send(event);
        } catch (Exception e) {
            log.debug("에러 이벤트 전송 실패", e);
        }
    }
}
