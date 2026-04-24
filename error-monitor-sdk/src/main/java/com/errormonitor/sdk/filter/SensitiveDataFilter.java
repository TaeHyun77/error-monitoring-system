package com.errormonitor.sdk.filter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SensitiveDataFilter {

    private static final String MASKED = "[FILTERED]";

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "x-api-key"
    );

    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "passwd", "secret", "token", "api_key",
            "apikey", "access_token", "refresh_token", "credit_card"
    );

    public Map<String, String> filterHeaders(Map<String, String> headers) {
        if (headers == null) return null;
        Map<String, String> filtered = new LinkedHashMap<>();
        headers.forEach((key, value) -> {
            if (SENSITIVE_HEADERS.contains(key.toLowerCase())) {
                filtered.put(key, MASKED);
            } else {
                filtered.put(key, value);
            }
        });
        return filtered;
    }

    public Map<String, String> filterParameters(Map<String, String> parameters) {
        if (parameters == null) return null;
        Map<String, String> filtered = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            if (SENSITIVE_PARAMS.contains(key.toLowerCase())) {
                filtered.put(key, MASKED);
            } else {
                filtered.put(key, value);
            }
        });
        return filtered;
    }
}
