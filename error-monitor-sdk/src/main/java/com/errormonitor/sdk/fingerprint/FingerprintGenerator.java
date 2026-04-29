package com.errormonitor.sdk.fingerprint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

// 같은 원인으로 발생한 에러를 하나의 그룹으로 묶기 위한 고유값을 생성하는 클래스
public class FingerprintGenerator {

    // 스택 트레이스에서 Spring, Java, Tomcat, Apache 같은 프레임워크 내부 호출을 제외하기 위한 PREFIX
    private static final Set<String> FRAMEWORK_PREFIXES = Set.of(
            "org.springframework.", "java.", "javax.", "jakarta.",
            "sun.", "com.sun.", "jdk.", "org.apache."
    );

    // "예외 타입 : 클래스명 : 메서드명"을 생성하고 해싱하여 같은 에러인지 판단
    public String generate(Exception exception) {
        String exceptionType = exception.getClass().getName();
        StackTraceElement appFrame = findFirstAppFrame(exception);

        String raw;
        if (appFrame != null) {
            raw = exceptionType + ":" + appFrame.getClassName() + ":" + appFrame.getMethodName();
        } else {
            raw = exceptionType + ":" + exception.getMessage();
        }

        return sha256(raw);
    }

    // 전체 스택트레이스에서 가장 먼저 등장하는 내 코드 프레임 찾기
    private StackTraceElement findFirstAppFrame(Exception exception) {
        for (StackTraceElement frame : exception.getStackTrace()) {
            if (!isFrameworkFrame(frame.getClassName())) {
                return frame;
            }
        }
        return null;
    }

    private boolean isFrameworkFrame(String className) {
        for (String prefix : FRAMEWORK_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // 해시 값 변환
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
