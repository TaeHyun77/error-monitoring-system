package com.errormonitor.sdk;

import com.errormonitor.sdk.capture.*;
import com.errormonitor.sdk.filter.SensitiveDataFilter;
import com.errormonitor.sdk.fingerprint.FingerprintGenerator;
import com.errormonitor.sdk.transport.FileBackupTransport;
import com.errormonitor.sdk.transport.HttpErrorTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// SDK의 핵심 자동 설정 클래스
// Spring Boot가 SDK jar를 읽을 때 이 설정 클래스를 자동으로 로드하고, 아래 Bean들을 등록합니다.
@Configuration
@EnableConfigurationProperties(ErrorMonitorProperties.class)
@ConditionalOnProperty(name = "error-monitor.enabled", matchIfMissing = true) // false 넣으면 전체 SDK 비활성화
public class ErrorMonitorAutoConfiguration {

    @Bean
    // 사용하는 프로젝트에서 사용자가 직접 Bean 만들면 SDK 기본 Bean 안씀 - 커스터 가능
    @ConditionalOnMissingBean
    public FingerprintGenerator fingerprintGenerator() { // 에러를 그룹핑하기 위한 고유 ID 생성
        return new FingerprintGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public SensitiveDataFilter sensitiveDataFilter() { // Authorization, password 같은 민감정보 제거
        return new SensitiveDataFilter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "errorMonitorRestTemplate")
    public RestTemplate errorMonitorRestTemplate(ErrorMonitorProperties properties) { // SDK 전용 HTTP 클라이언트
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());
        return new RestTemplate(factory);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileBackupTransport fileBackupTransport(ErrorMonitorProperties properties) { // 전송 실패 시 로컬 JSON 저장
        return new FileBackupTransport(properties.getBackupDir());
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpErrorTransport httpErrorTransport( // Error Server로 HTTP 전송
            @Qualifier("errorMonitorRestTemplate") RestTemplate errorMonitorRestTemplate,
            ErrorMonitorProperties properties,
            FileBackupTransport fileBackupTransport
    ) {
        return new HttpErrorTransport(
                errorMonitorRestTemplate,
                properties.getServerUrl(),
                properties.getApiKey(),
                fileBackupTransport
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorCaptor errorCaptor( // ⭐ 핵심
            HttpErrorTransport httpErrorTransport,
            FileBackupTransport fileBackupTransport,
            FingerprintGenerator fingerprintGenerator,
            SensitiveDataFilter sensitiveDataFilter,
            ErrorMonitorProperties properties
    ) {
        return new ErrorCaptor(
                httpErrorTransport,
                fileBackupTransport,
                fingerprintGenerator,
                sensitiveDataFilter,
                properties.getProjectId(),
                properties.getEnvironment(),
                properties.getQueueCapacity(),
                properties.getMaxStackFrames(),
                properties.getMaxStackTraceBytes(),
                properties.getIgnoreExceptions(),
                properties.getGithubRepo(),
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getShutdownTimeout()
        );
    }

    // HTTP 요청 예외 감지
    @Bean
    @ConditionalOnMissingBean
    public ExceptionInterceptor exceptionInterceptor(ErrorCaptor errorCaptor, ErrorMonitorProperties properties) {
        return new ExceptionInterceptor(errorCaptor, properties.getIgnoreUrls());
    }

    // Spring MVC에 인터셉터 등록하여 모든 요청의 예외 감지
    @Bean
    @ConditionalOnMissingBean
    public WebMvcInterceptorConfig webMvcInterceptorConfig(ExceptionInterceptor exceptionInterceptor) {
        return new WebMvcInterceptorConfig(exceptionInterceptor);
    }

    // 로그 기반 예외 감지 : log.error() → SDK로 들어옴
    @Bean
    @ConditionalOnMissingBean
    public LogbackErrorAppender logbackErrorAppender(ErrorCaptor errorCaptor) {
        LogbackErrorAppender appender = new LogbackErrorAppender(errorCaptor);
        appender.register();
        return appender;
    }

    // 필터 레이어 예외 감지 : 4xx(의도된 오류) 제외, 5xx급 장애만 포착
    @Bean
    @ConditionalOnMissingBean(FilterExceptionCaptureFilter.class)
    public FilterRegistrationBean<FilterExceptionCaptureFilter> filterExceptionCaptureFilter(
            ErrorCaptor errorCaptor,
            ErrorMonitorProperties properties) {
        FilterExceptionCaptureFilter filter =
                new FilterExceptionCaptureFilter(errorCaptor, properties.getIgnoreUrls());
        FilterRegistrationBean<FilterExceptionCaptureFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // 필터 체인 최외곽에 배치
        return registration;
    }
}
