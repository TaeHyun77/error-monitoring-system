package com.errormonitor.sdk.stackTrace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StackFrameInfo {
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;
}
