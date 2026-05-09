package com.errormonitor.sdk.capture;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

// 예외 객체 기반 4xx 판별 유틸. Filter와 Interceptor가 공유한다.
// catch 시점에 response.getStatus()가 미확정일 때,
// 예외 자체에서 의도된 4xx 여부를 판단하는 용도.
public class HttpStatusExceptionUtils {

    private HttpStatusExceptionUtils() {
    }

    public static boolean is4xxException(Exception e) {
        if (e instanceof ResponseStatusException rse) {
            return rse.getStatusCode().is4xxClientError();
        }
        ResponseStatus annotation = e.getClass().getAnnotation(ResponseStatus.class);
        return annotation != null && annotation.value().is4xxClientError();
    }
}
