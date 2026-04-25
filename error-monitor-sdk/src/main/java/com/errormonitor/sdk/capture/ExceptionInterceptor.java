package com.errormonitor.sdk.capture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

public class ExceptionInterceptor implements HandlerInterceptor {

    private static final String DISPATCHER_EXCEPTION_ATTR =
            "org.springframework.web.servlet.DispatcherServlet.EXCEPTION";

    private final ErrorCaptor errorCaptor;
    private final List<String> ignoreUrls;

    public ExceptionInterceptor(ErrorCaptor errorCaptor, List<String> ignoreUrls) {
        this.errorCaptor = errorCaptor;
        this.ignoreUrls = ignoreUrls;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String requestUri = request.getRequestURI();
        if (ignoreUrls.stream().anyMatch(requestUri::startsWith)) {
            return;
        }

        Exception exception = ex;

        if (exception == null) {
            Object attr = request.getAttribute(DISPATCHER_EXCEPTION_ATTR);
            if (attr instanceof Exception) {
                exception = (Exception) attr;
            }
        }

        if (exception != null) {
            errorCaptor.captureException(exception, request);
        }
    }
}
