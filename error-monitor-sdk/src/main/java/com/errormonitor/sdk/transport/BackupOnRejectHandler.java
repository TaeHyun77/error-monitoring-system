package com.errormonitor.sdk.transport;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class BackupOnRejectHandler implements RejectedExecutionHandler {
    private final FileBackupTransport fileBackupTransport;

    public BackupOnRejectHandler(FileBackupTransport fileBackupTransport) {
        this.fileBackupTransport = fileBackupTransport;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.warn("에러 전송 큐 초과 - 로컬 백업으로 전환");
        if (r instanceof ErrorSendTask task) {
            fileBackupTransport.backup(task.getEvent());
        }
    }
}
