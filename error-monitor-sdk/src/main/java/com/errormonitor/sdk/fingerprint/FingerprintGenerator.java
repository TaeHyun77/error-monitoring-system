package com.errormonitor.sdk.fingerprint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class FingerprintGenerator {

    private static final Set<String> FRAMEWORK_PREFIXES = Set.of(
            "org.springframework.", "java.", "javax.", "jakarta.",
            "sun.", "com.sun.", "jdk.", "org.apache."
    );

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
