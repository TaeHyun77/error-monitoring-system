package com.errormonitor.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ErrorEvent { // 하나의 에러를 나타내는 정보 값
    private String projectId; // 프로젝트 구분
    private String fingerprint; // 같은 원인의 에러를 식별하는 고유 ID
    private ExceptionInfo exceptionInfo; // 예외 정보
    private RequestContext requestContext; // 요청 정보
    private Instant timestamp; // 에러 발생 시각
    private String environment; // 프로젝트의 환경 정보 - local / dev / staging / production
    private String githubRepo; // 프로젝트의 깃허브 리포
}
