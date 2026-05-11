package com.errormonitor.sdk.event;

import com.errormonitor.sdk.stackTrace.StackFrameInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionInfo {
    private String type;
    private String message;
    private List<StackFrameInfo> stackFrames;
    private String rawStackTrace;
}