package com.errormonitor.server.error.event.dto;

import com.errormonitor.server.error.event.ErrorEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ErrorEventResDto {
    private Long id;
    private String projectId;
    private Long errorGroupId;
    private String exceptionType;
    private String message;
    private String stackTrace;
    private String sourceClass;
    private String sourceMethod;
    private Integer sourceLineNumber;
    private String requestMethod;
    private String requestUrl;
    private String requestHeaders;
    private String requestParameters;
    private String clientIp;
    private String environment;
    private LocalDateTime createdAt;

    public static ErrorEventResDto from(ErrorEvent event) {
        return ErrorEventResDto.builder()
                .id(event.getId())
                .projectId(event.getProjectId())
                .errorGroupId(event.getErrorGroup().getId())
                .exceptionType(event.getExceptionType())
                .message(event.getMessage())
                .stackTrace(event.getStackTrace())
                .sourceClass(event.getSourceClass())
                .sourceMethod(event.getSourceMethod())
                .sourceLineNumber(event.getSourceLineNumber())
                .requestMethod(event.getRequestMethod())
                .requestUrl(event.getRequestUrl())
                .requestHeaders(event.getRequestHeaders())
                .requestParameters(event.getRequestParameters())
                .clientIp(event.getClientIp())
                .environment(event.getEnvironment())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
