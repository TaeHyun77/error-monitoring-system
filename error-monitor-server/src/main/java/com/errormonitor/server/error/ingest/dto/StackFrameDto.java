package com.errormonitor.server.error.ingest.dto;

import lombok.Getter;

@Getter
public class StackFrameDto {
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;
}
