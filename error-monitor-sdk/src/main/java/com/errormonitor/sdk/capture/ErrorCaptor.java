package com.errormonitor.sdk.capture;

import com.errormonitor.sdk.filter.SensitiveDataFilter;
import com.errormonitor.sdk.fingerprint.FingerprintGenerator;
import com.errormonitor.sdk.model.ErrorEvent;
import com.errormonitor.sdk.model.ExceptionInfo;
import com.errormonitor.sdk.model.RequestContext;
import com.errormonitor.sdk.transport.BackupOnRejectHandler;
import com.errormonitor.sdk.transport.ErrorSendTask;
import com.errormonitor.sdk.transport.FileBackupTransport;
import com.errormonitor.sdk.transport.HttpErrorTransport;
import com.errormonitor.sdk.stackTrace.StackTraceParser;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ErrorCaptor { //
    private final HttpErrorTransport transport;
    private final FileBackupTransport fileBackupTransport;
    private final FingerprintGenerator fingerprintGenerator;
    private final SensitiveDataFilter sensitiveDataFilter;
    private final String projectId;
    private final String environment;
    private final int maxStackFrames;
    private final int maxStackTraceBytes;
    private final List<String> ignoreExceptions;
    private final String githubRepo;
    private final int shutdownTimeout;

    // 같은 Exception 객체가 Interceptor와 Logback에서 중복 캡처되는 것을 방지
    private final Set<Exception> capturedExceptions =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final ThreadPoolExecutor executor;

    public ErrorCaptor(
            HttpErrorTransport transport,
            FileBackupTransport fileBackupTransport,
            FingerprintGenerator fingerprintGenerator,
            SensitiveDataFilter sensitiveDataFilter,
            String projectId,
            String environment,
            int queueCapacity,
            int maxStackFrames,
            int maxStackTraceBytes,
            List<String> ignoreExceptions,
            String githubRepo,
            int corePoolSize,
            int maxPoolSize,
            int shutdownTimeout
    ) {
        this.transport = transport;
        this.fileBackupTransport = fileBackupTransport;
        this.fingerprintGenerator = fingerprintGenerator;
        this.sensitiveDataFilter = sensitiveDataFilter;
        this.projectId = projectId;
        this.environment = environment;
        this.maxStackFrames = maxStackFrames;
        this.maxStackTraceBytes = maxStackTraceBytes;
        this.ignoreExceptions = ignoreExceptions;
        this.githubRepo = githubRepo;
        this.shutdownTimeout = shutdownTimeout;

        AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory daemonThreadFactory = r -> {
            Thread t = new Thread(r, "error-monitor-sender-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        };

        this.executor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                daemonThreadFactory,
                new BackupOnRejectHandler(fileBackupTransport)
        );
    }

    // 예외가 ExceptionInterceptor와 LogbackErrorAppender 두 군데에서 동시에 잡힐 수 있기에, 한 번만 처리되도록 함
    public void captureException(Exception exception, HttpServletRequest request) {
        if (exception == null) return;
        if (shouldIgnore(exception)) return;
        if (isAlreadyCaptured(exception)) return;

        try {
            ErrorEvent event = buildEvent(exception, request);
            executor.submit(new ErrorSendTask(transport, event));
        } catch (Exception e) {
            log.debug("에러 캡처 실패", e);
        }
    }

    // 이벤트 생성
    public void captureException(Exception exception) {
        if (exception == null) return;
        if (shouldIgnore(exception)) return;
        if (isAlreadyCaptured(exception)) return;

        try {
            ErrorEvent event = buildEvent(exception, null);
            executor.submit(new ErrorSendTask(transport, event));
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
                // 리스트 형태의 INFO 형태인 스택 트레이스와 길이를 줄인 원본 문자열 함께 서버로 보냄
                .stackFrames(StackTraceParser.parseFrames(exception, maxStackFrames))
                .rawStackTrace(StackTraceParser.truncate(exception, maxStackTraceBytes))
                .build();

        RequestContext requestContext = request != null ? buildRequestContext(request) : null;

        return ErrorEvent.builder()
                .projectId(projectId)
                .fingerprint(fingerprintGenerator.generate(exception))
                .exceptionInfo(exceptionInfo)
                .requestContext(requestContext)
                .timestamp(Instant.now())
                .environment(environment)
                .githubRepo(githubRepo)
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
        log.info("에러 모니터 SDK 종료 - 큐 잔여: {}, 활성: {}",
                executor.getQueue().size(), executor.getActiveCount());

        executor.shutdown();

        try {
            if (!executor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
                List<Runnable> dropped = executor.shutdownNow();
                for (Runnable r : dropped) {
                    if (r instanceof ErrorSendTask task) {
                        fileBackupTransport.backup(task.getEvent());
                    }
                }
                log.warn("에러 모니터 SDK 강제 종료 - 미처리 작업 {} 건 백업 완료", dropped.size());
            }
        } catch (InterruptedException e) {
            List<Runnable> dropped = executor.shutdownNow();
            for (Runnable r : dropped) {
                if (r instanceof ErrorSendTask task) {
                    fileBackupTransport.backup(task.getEvent());
                }
            }
            Thread.currentThread().interrupt();
        }
    }
}
