package com.errormonitor.server.error.ingest.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ExceptionInfoDto {
    private String type;
    private String message;
    private List<StackFrameDto> stackFrames;
    private String rawStackTrace;
}
