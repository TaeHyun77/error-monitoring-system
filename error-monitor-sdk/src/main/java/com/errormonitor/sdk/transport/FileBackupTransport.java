package com.errormonitor.sdk.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.errormonitor.sdk.model.ErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileBackupTransport {

    private static final Logger log = LoggerFactory.getLogger(FileBackupTransport.class);
    private final ObjectMapper objectMapper;
    private final Path backupDir;

    public FileBackupTransport(String backupFilePath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.backupDir = Paths.get(backupFilePath);
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            log.warn("백업 디렉토리 생성 실패: {}", backupFilePath, e);
        }
    }

    public void backup(ErrorEvent event) {
        try {
            String fileName = "error_" + System.currentTimeMillis() + ".json";
            Path filePath = backupDir.resolve(fileName);
            objectMapper.writeValue(filePath.toFile(), event);
            log.debug("에러 이벤트 백업 완료: {}", filePath);
        } catch (Exception e) {
            log.warn("에러 이벤트 백업 실패", e);
        }
    }
}
