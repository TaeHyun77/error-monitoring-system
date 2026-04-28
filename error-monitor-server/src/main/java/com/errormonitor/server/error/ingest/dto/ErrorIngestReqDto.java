package com.errormonitor.server.error.ingest.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ErrorIngestReqDto {
    private String projectId;
    private String fingerprint;
    private ExceptionInfoDto exceptionInfo;
    private RequestContextDto requestContext;
    private Instant timestamp;
    private String environment;
    private String githubRepo;
}
