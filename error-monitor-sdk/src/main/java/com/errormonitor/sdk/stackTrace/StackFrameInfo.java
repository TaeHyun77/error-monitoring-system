package com.errormonitor.sdk.stackTrace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StackFrameInfo {
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;
}
