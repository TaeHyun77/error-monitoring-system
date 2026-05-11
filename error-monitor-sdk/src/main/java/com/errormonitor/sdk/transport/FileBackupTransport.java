package com.errormonitor.sdk.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.errormonitor.sdk.event.ErrorEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FileBackupTransport {
    private static final AtomicLong counter = new AtomicLong(0);
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
            String fileName = "error_" + System.currentTimeMillis() + "_" + counter.getAndIncrement() + ".json";
            Path filePath = backupDir.resolve(fileName);
            objectMapper.writeValue(filePath.toFile(), event);
            log.debug("에러 이벤트 백업 완료: {}", filePath);
        } catch (Exception e) {
            log.warn("에러 이벤트 백업 실패", e);
        }
    }

    public List<Path> listBackupFiles() {
        try (Stream<Path> stream = Files.list(backupDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("백업 파일 목록 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ErrorEvent readBackupFile(Path filePath) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            return objectMapper.readValue(bytes, ErrorEvent.class);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    public void deleteBackupFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            log.debug("백업 파일 삭제 완료: {}", filePath);
        } catch (IOException e) {
            log.warn("백업 파일 삭제 실패: {}", filePath, e);
        }
    }

    public void moveToDeadLetter(Path filePath) {
        try {
            Path deadLetterDir = backupDir.resolve("dead-letter");
            Files.createDirectories(deadLetterDir);
            Path target = deadLetterDir.resolve(filePath.getFileName());
            Files.move(filePath, target, StandardCopyOption.REPLACE_EXISTING);
            log.warn("백업 파일을 dead-letter로 이동: {}", filePath.getFileName());
        } catch (IOException e) {
            log.warn("dead-letter 이동 실패: {}", filePath, e);
        }
    }
}
