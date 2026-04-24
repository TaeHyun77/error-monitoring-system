package com.errormonitor.sdk.capture;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class WebMvcInterceptorConfig implements WebMvcConfigurer {

    private final ExceptionInterceptor exceptionInterceptor;

    public WebMvcInterceptorConfig(ExceptionInterceptor exceptionInterceptor) {
        this.exceptionInterceptor = exceptionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(exceptionInterceptor).addPathPatterns("/**");
    }
}
