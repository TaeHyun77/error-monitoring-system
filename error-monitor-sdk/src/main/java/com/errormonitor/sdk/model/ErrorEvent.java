package com.errormonitor.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ErrorEvent {
    private String projectId;
    private String fingerprint;
    private ExceptionInfo exceptionInfo;
    private RequestContext requestContext;
    private Instant timestamp;
    private String environment;
    private String githubRepo;
}
