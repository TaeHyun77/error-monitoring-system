package com.errormonitor.sdk.capture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

public class LogbackErrorAppender extends AppenderBase<ILoggingEvent> {

    private static final String SDK_LOGGER_PREFIX = "com.errormonitor.sdk";

    private final ErrorCaptor errorCaptor;

    public LogbackErrorAppender(ErrorCaptor errorCaptor) {
        this.errorCaptor = errorCaptor;
        setName("ERROR_MONITOR_APPENDER");
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            if (!Level.ERROR.equals(event.getLevel())) {
                return;
            }

            if (event.getLoggerName().startsWith(SDK_LOGGER_PREFIX)) {
                return;
            }

            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy instanceof ThrowableProxy proxy) {
                Throwable throwable = proxy.getThrowable();
                if (throwable instanceof Exception exception) {
                    errorCaptor.captureException(exception);
                }
            }
        } catch (Exception e) {
            // SDK 오류가 메인 앱에 영향을 주면 안 됨
        }
    }

    public void register() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(context);
        start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
    }
}
