package com.errormonitor.server.error.group.dto;

import com.errormonitor.server.error.group.ErrorGroup;
import com.errormonitor.server.error.group.ErrorGroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ErrorGroupResDto {
    private Long id;
    private String projectId;
    private String fingerprint;
    private String exceptionType;
    private String message;
    private String sourceClass;
    private String sourceMethod;
    private Integer sourceLineNumber;
    private long eventCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private ErrorGroupStatus status;

    public static ErrorGroupResDto from(ErrorGroup group) {
        return ErrorGroupResDto.builder()
                .id(group.getId())
                .projectId(group.getProjectId())
                .fingerprint(group.getFingerprint())
                .exceptionType(group.getExceptionType())
                .message(group.getMessage())
                .sourceClass(group.getSourceClass())
                .sourceMethod(group.getSourceMethod())
                .sourceLineNumber(group.getSourceLineNumber())
                .eventCount(group.getEventCount())
                .firstSeen(group.getFirstSeen())
                .lastSeen(group.getLastSeen())
                .status(group.getStatus())
                .build();
    }
}
