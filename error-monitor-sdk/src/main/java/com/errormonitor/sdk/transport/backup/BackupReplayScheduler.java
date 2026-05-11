package com.errormonitor.sdk.transport.backup;

import com.errormonitor.sdk.event.ErrorEvent;
import com.errormonitor.sdk.transport.FileBackupTransport;
import com.errormonitor.sdk.transport.HttpErrorTransport;
import com.errormonitor.sdk.transport.SendResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class BackupReplayScheduler {
    private final FileBackupTransport fileBackupTransport;
    private final HttpErrorTransport httpErrorTransport;
    private final int replayIntervalSeconds;
    private final int replayBatchSize;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "error-monitor-replay");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleWithFixedDelay(
                this::replaySafely,
                replayIntervalSeconds,
                replayIntervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("백업 재전송 스케줄러 시작 (간격: {}초, 배치: {}건)",
                replayIntervalSeconds, replayBatchSize);
    }

    private void replaySafely() {
        try {
            replay();
        } catch (Exception e) {
            log.warn("백업 재전송 중 예외 발생", e);
        }
    }

    void replay() {
        List<Path> files = fileBackupTransport.listBackupFiles();

        if (files.isEmpty()) {
            return;
        }

        int limit = Math.min(files.size(), replayBatchSize);
        log.debug("백업 재전송 시작: {}건 대상 (전체 {}건)", limit, files.size());

        for (int i = 0; i < limit; i++) {
            Path filePath = files.get(i);

            ErrorEvent event;
            try {
                event = fileBackupTransport.readBackupFile(filePath);
            } catch (IOException e) {
                log.warn("백업 파일 역직렬화 실패, dead-letter로 이동: {}", filePath.getFileName());
                fileBackupTransport.moveToDeadLetter(filePath);
                continue;
            }

            if (event == null) {
                continue;
            }

            SendResult result = httpErrorTransport.trySend(event);

            switch (result) {
                case SUCCESS:
                    fileBackupTransport.deleteBackupFile(filePath);
                    break;

                case CLIENT_ERROR:
                    fileBackupTransport.moveToDeadLetter(filePath);
                    break;

                case SERVER_ERROR:
                    log.debug("서버 오류로 재전송 중단, 다음 주기에 재시도");
                    return;
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler == null) {
            return;
        }
        log.info("백업 재전송 스케줄러 종료 중...");
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}