package com.errormonitor.sdk;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

// SDK 설정값을 관리하는 클래스
// SDK를 사용하는 프로젝트가 application.yml/properties에서 설정한 값을 SDK 내부 Bean들이 사용할 수 있게 전달
@Getter
@Setter
@ConfigurationProperties(prefix = "error-monitor")
public class ErrorMonitorProperties {
    private boolean enabled = true;
    private String serverUrl = "http://localhost:8090";
    private String projectId = "default";
    private String apiKey = "";
    private String environment = "production";
    private int queueCapacity = 100;
    private int maxStackFrames = 50;
    private int maxStackTraceBytes = 10240;
    private String backupDir = "error-backup";
    private String githubRepo = "";
    private List<String> ignoreExceptions = new ArrayList<>();
    private List<String> ignoreUrls = new ArrayList<>();
    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int connectTimeout = 3000;
    private int readTimeout = 5000;
    private int shutdownTimeout = 10;
}
