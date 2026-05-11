package com.errormonitor.sdk.capture;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
public class WebMvcInterceptorConfig implements WebMvcConfigurer {
    private final ExceptionInterceptor exceptionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(exceptionInterceptor).addPathPatterns("/**");
    }
}
