package com.errormonitor.sdk;

import com.errormonitor.sdk.capture.*;
import com.errormonitor.sdk.filter.SensitiveDataFilter;
import com.errormonitor.sdk.fingerprint.FingerprintGenerator;
import com.errormonitor.sdk.transport.FileBackupTransport;
import com.errormonitor.sdk.transport.HttpErrorTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ErrorMonitorProperties.class)
@ConditionalOnProperty(name = "error-monitor.enabled", matchIfMissing = true)
public class ErrorMonitorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FingerprintGenerator fingerprintGenerator() {
        return new FingerprintGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public SensitiveDataFilter sensitiveDataFilter() {
        return new SensitiveDataFilter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "errorMonitorRestTemplate")
    public RestTemplate errorMonitorRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public FileBackupTransport fileBackupTransport(ErrorMonitorProperties properties) {
        return new FileBackupTransport(properties.getBackupDir());
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpErrorTransport httpErrorTransport(@Qualifier("errorMonitorRestTemplate") RestTemplate errorMonitorRestTemplate,
                                                  ErrorMonitorProperties properties,
                                                  FileBackupTransport fileBackupTransport) {
        return new HttpErrorTransport(
                errorMonitorRestTemplate,
                properties.getServerUrl(),
                properties.getApiKey(),
                fileBackupTransport
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorCaptor errorCaptor(HttpErrorTransport httpErrorTransport,
                                   FingerprintGenerator fingerprintGenerator,
                                   SensitiveDataFilter sensitiveDataFilter,
                                   ErrorMonitorProperties properties) {
        return new ErrorCaptor(
                httpErrorTransport,
                fingerprintGenerator,
                sensitiveDataFilter,
                properties.getProjectId(),
                properties.getEnvironment(),
                properties.getQueueCapacity(),
                properties.getMaxStackFrames(),
                properties.getMaxStackTraceBytes(),
                properties.getIgnoreExceptions()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionInterceptor exceptionInterceptor(ErrorCaptor errorCaptor, ErrorMonitorProperties properties) {
        return new ExceptionInterceptor(errorCaptor, properties.getIgnoreUrls());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebMvcInterceptorConfig webMvcInterceptorConfig(ExceptionInterceptor exceptionInterceptor) {
        return new WebMvcInterceptorConfig(exceptionInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogbackErrorAppender logbackErrorAppender(ErrorCaptor errorCaptor) {
        LogbackErrorAppender appender = new LogbackErrorAppender(errorCaptor);
        appender.register();
        return appender;
    }
}
