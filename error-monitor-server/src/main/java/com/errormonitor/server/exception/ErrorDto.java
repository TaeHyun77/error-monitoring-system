package com.errormonitor.server.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorDto {
    private String code;
    private String msg;
    private String detail;
}
