package com.errormonitor.server.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MonitorException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;

    public MonitorException(HttpStatus httpStatus, ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
