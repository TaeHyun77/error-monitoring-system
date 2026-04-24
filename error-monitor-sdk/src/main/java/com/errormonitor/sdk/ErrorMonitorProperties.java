package com.errormonitor.sdk;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
