package com.errormonitor.server.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // NOT_FOUND
    PROJECT_NOT_FOUND("등록된 프로젝트를 찾을 수 없습니다"),
    ERROR_GROUP_NOT_FOUND("에러 그룹을 찾을 수 없습니다"),
    ERROR_EVENT_NOT_FOUND("에러 이벤트를 찾을 수 없습니다"),

    // AUTH
    INVALID_API_KEY("유효하지 않은 API Key입니다"),

    // CONFLICT
    DUPLICATE_PROJECT_ID("이미 존재하는 프로젝트 ID입니다"),

    // ANALYSIS
    ANALYSIS_IN_PROGRESS("이미 분석이 진행 중입니다");

    private final String message;
}
