package com.errormonitor.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ExceptionInfo {
    private String type;
    private String message;
    private List<StackFrameInfo> stackFrames;
    private String rawStackTrace;
}
