package com.errormonitor.sdk.capture;

import com.errormonitor.sdk.filter.SensitiveDataFilter;
import com.errormonitor.sdk.fingerprint.FingerprintGenerator;
import com.errormonitor.sdk.model.ErrorEvent;
import com.errormonitor.sdk.model.ExceptionInfo;
import com.errormonitor.sdk.model.RequestContext;
import com.errormonitor.sdk.transport.HttpErrorTransport;
import com.errormonitor.sdk.util.StackTraceUtils;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ErrorCaptor {

    private static final Logger log = LoggerFactory.getLogger(ErrorCaptor.class);

    private final HttpErrorTransport transport;
    private final FingerprintGenerator fingerprintGenerator;
    private final SensitiveDataFilter sensitiveDataFilter;
    private final String projectId;
    private final String environment;
    private final int maxStackFrames;
    private final int maxStackTraceBytes;
    private final List<String> ignoreExceptions;

    // 같은 Exception 객체가 Interceptor와 Logback에서 중복 캡처되는 것을 방지
    private final Set<Exception> capturedExceptions =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final ExecutorService executor;

    public ErrorCaptor(HttpErrorTransport transport,
                       FingerprintGenerator fingerprintGenerator,
                       SensitiveDataFilter sensitiveDataFilter,
                       String projectId,
                       String environment,
                       int queueCapacity,
                       int maxStackFrames,
                       int maxStackTraceBytes,
                       List<String> ignoreExceptions) {
        this.transport = transport;
        this.fingerprintGenerator = fingerprintGenerator;
        this.sensitiveDataFilter = sensitiveDataFilter;
        this.projectId = projectId;
        this.environment = environment;
        this.maxStackFrames = maxStackFrames;
        this.maxStackTraceBytes = maxStackTraceBytes;
        this.ignoreExceptions = ignoreExceptions;
        this.executor = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    public void captureException(Exception exception, HttpServletRequest request) {
        if (exception == null) return;
        if (shouldIgnore(exception)) return;
        if (isAlreadyCaptured(exception)) return;

        try {
            ErrorEvent event = buildEvent(exception, request);
            executor.submit(() -> {
                try {
                    transport.send(event);
                } catch (Exception e) {
                    log.debug("에러 이벤트 전송 실패", e);
                }
            });
        } catch (Exception e) {
            log.debug("에러 캡처 실패", e);
        }
    }

    public void captureException(Exception exception) {
        if (exception == null) return;
        if (shouldIgnore(exception)) return;
        if (isAlreadyCaptured(exception)) return;

        try {
            ErrorEvent event = buildEvent(exception, null);
            executor.submit(() -> {
                try {
                    transport.send(event);
                } catch (Exception e) {
                    log.debug("에러 이벤트 전송 실패", e);
                }
            });
        } catch (Exception e) {
            log.debug("에러 캡처 실패", e);
        }
    }

    private boolean isAlreadyCaptured(Exception exception) {
        synchronized (capturedExceptions) {
            if (capturedExceptions.contains(exception)) {
                return true;
            }
            capturedExceptions.add(exception);
            return false;
        }
    }

    private boolean shouldIgnore(Exception exception) {
        String exceptionName = exception.getClass().getName();
        return ignoreExceptions != null
                && ignoreExceptions.stream().anyMatch(exceptionName::contains);
    }

    private ErrorEvent buildEvent(Exception exception, HttpServletRequest request) {
        ExceptionInfo exceptionInfo = ExceptionInfo.builder()
                .type(exception.getClass().getName())
                .message(exception.getMessage())
                .stackFrames(StackTraceUtils.parseFrames(exception, maxStackFrames))
                .rawStackTrace(StackTraceUtils.truncate(exception, maxStackTraceBytes))
                .build();

        RequestContext requestContext = request != null ? buildRequestContext(request) : null;

        return ErrorEvent.builder()
                .projectId(projectId)
                .fingerprint(fingerprintGenerator.generate(exception))
                .exceptionInfo(exceptionInfo)
                .requestContext(requestContext)
                .timestamp(Instant.now())
                .environment(environment)
                .build();
    }

    private RequestContext buildRequestContext(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }
        }

        Map<String, String> parameters = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) ->
                parameters.put(key, values.length > 0 ? values[0] : ""));

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }

        return RequestContext.builder()
                .method(request.getMethod())
                .url(request.getRequestURL().toString())
                .headers(sensitiveDataFilter.filterHeaders(headers))
                .parameters(sensitiveDataFilter.filterParameters(parameters))
                .clientIp(clientIp)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}