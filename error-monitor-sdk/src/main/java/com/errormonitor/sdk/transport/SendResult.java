package com.errormonitor.sdk.transport;

public enum SendResult {
    SUCCESS,       // 전송 성공
    CLIENT_ERROR,  // 4xx — 서버가 이벤트를 거부 (재시도 무의미)
    SERVER_ERROR   // 5xx / 네트워크 오류 — 서버 다운 (재시도 의미 있음)
}
