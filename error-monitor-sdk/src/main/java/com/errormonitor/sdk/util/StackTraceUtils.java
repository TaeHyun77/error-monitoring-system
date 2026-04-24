package com.errormonitor.sdk.util;

import com.errormonitor.sdk.model.StackFrameInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class StackTraceUtils {

    private static final int DEFAULT_MAX_FRAMES = 50;
    private static final int DEFAULT_MAX_BYTES = 10240;

    public static List<StackFrameInfo> parseFrames(Exception exception, int maxFrames) {
        StackTraceElement[] elements = exception.getStackTrace();
        int limit = Math.min(elements.length, maxFrames);
        List<StackFrameInfo> frames = new ArrayList<>(limit);

        for (int i = 0; i < limit; i++) {
            StackTraceElement el = elements[i];
            frames.add(StackFrameInfo.builder()
                    .className(el.getClassName())
                    .methodName(el.getMethodName())
                    .fileName(el.getFileName())
                    .lineNumber(el.getLineNumber())
                    .build());
        }
        return frames;
    }

    public static List<StackFrameInfo> parseFrames(Exception exception) {
        return parseFrames(exception, DEFAULT_MAX_FRAMES);
    }

    public static String truncate(Exception exception, int maxBytes) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();

        if (full.length() <= maxBytes) {
            return full;
        }
        return full.substring(0, maxBytes) + "\n... [truncated]";
    }

    public static String truncate(Exception exception) {
        return truncate(exception, DEFAULT_MAX_BYTES);
    }
}
