package com.errormonitor.sdk.stackTrace;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

// Exception의 스택트레이스를 전송용 데이터로 가공하는 유틸 클래스
public class StackTraceParser {

    private static final int DEFAULT_MAX_FRAMES = 50; // 최대 프레임 수: 50개
    private static final int DEFAULT_MAX_BYTES = 10240; // 최대 문자열 길이: 10KB

    // 스택트레이스를 객체 리스트 형태로 변환
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
                    .build()
            );
        }
        return frames;
    }

    // maxFrames 기본 설정 값
    public static List<StackFrameInfo> parseFrames(Exception exception) {
        return parseFrames(exception, DEFAULT_MAX_FRAMES);
    }

    // Exception의 전체 스택트레이스를 문자열로 만든 뒤, 지정한 길이( maxBytes )까지만 남기고 잘라냄
    public static String truncate(Exception exception, int maxBytes) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();

        if (full.length() <= maxBytes) {
            return full;
        }
        return full.substring(0, maxBytes) + "\n... [truncated]";
    }

    // maxBytes 기본 설정 값
    public static String truncate(Exception exception) {
        return truncate(exception, DEFAULT_MAX_BYTES);
    }
}
