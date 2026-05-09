package com.errormonitor.sdk.capture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LogbackErrorAppenderTest {

    private ErrorCaptor errorCaptor;
    private LogbackErrorAppender appender;

    @BeforeEach
    void setUp() {
        errorCaptor = mock(ErrorCaptor.class);
        appender = new LogbackErrorAppender(errorCaptor);
        appender.start();
    }

    // 검증 6: log.error()로 4xx 예외를 남기면 LogbackAppender에서 캡처됨 (의도된 동작)
    @Test
    void log_error_4xx_예외_캡처됨() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "없음");
        ILoggingEvent event = mockEvent(Level.ERROR, "com.example.SomeService", exception);

        appender.doAppend(event);

        verify(errorCaptor).captureException(eq(exception));
    }

    // log.error()로 @ResponseStatus(BAD_REQUEST) 예외를 남기면 캡처됨
    @Test
    void log_error_ResponseStatus_4xx_어노테이션_예외_캡처됨() {
        BadRequestException exception = new BadRequestException();
        ILoggingEvent event = mockEvent(Level.ERROR, "com.example.SomeService", exception);

        appender.doAppend(event);

        verify(errorCaptor).captureException(eq(exception));
    }

    // log.error()로 5xx 예외를 남기면 캡처됨
    @Test
    void log_error_5xx_예외_캡처됨() {
        RuntimeException exception = new RuntimeException("서버 오류");
        ILoggingEvent event = mockEvent(Level.ERROR, "com.example.SomeService", exception);

        appender.doAppend(event);

        verify(errorCaptor).captureException(eq(exception));
    }

    // log.warn()은 캡처 안 됨
    @Test
    void log_warn_캡처_안됨() {
        RuntimeException exception = new RuntimeException("경고");
        ILoggingEvent event = mockEvent(Level.WARN, "com.example.SomeService", exception);

        appender.doAppend(event);

        verify(errorCaptor, never()).captureException(any(Exception.class));
    }

    // SDK 자체 로거는 캡처 안 됨
    @Test
    void SDK_로거_캡처_안됨() {
        RuntimeException exception = new RuntimeException("SDK 내부 오류");
        ILoggingEvent event = mockEvent(Level.ERROR, "com.errormonitor.sdk.transport.HttpErrorTransport", exception);

        appender.doAppend(event);

        verify(errorCaptor, never()).captureException(any(Exception.class));
    }

    // 예외 없는 log.error()는 캡처 안 됨
    @Test
    void log_error_예외_없으면_캡처_안됨() {
        ILoggingEvent event = mockEvent(Level.ERROR, "com.example.SomeService", null);

        appender.doAppend(event);

        verify(errorCaptor, never()).captureException(any(Exception.class));
    }

    private ILoggingEvent mockEvent(Level level, String loggerName, Exception exception) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(level);
        when(event.getLoggerName()).thenReturn(loggerName);
        if (exception != null) {
            when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(exception));
        }
        return event;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class BadRequestException extends RuntimeException {}
}
