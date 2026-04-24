package com.errormonitor.server.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(MonitorException.class)
    public ResponseEntity<ErrorDto> handleMonitorException(MonitorException e) {
        ErrorDto errorDto = ErrorDto.builder()
                .code(e.getErrorCode().name())
                .msg(e.getErrorCode().getMessage())
                .build();
        return ResponseEntity.status(e.getHttpStatus()).body(errorDto);
    }
}
