package com.errormonitor.server.error.event;

import com.errormonitor.server.common.BaseTime;
import com.errormonitor.server.error.group.ErrorGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "error_events", indexes = {
        @Index(name = "idx_error_event_project_created", columnList = "projectId, createdAt DESC"),
        @Index(name = "idx_error_event_group", columnList = "error_group_id")
})
public class ErrorEvent extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_group_id", nullable = false)
    private ErrorGroup errorGroup;

    @Column(nullable = false)
    private String exceptionType;

    @Column(length = 1000)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    private String requestMethod;

    @Column(length = 2000)
    private String requestUrl;

    @Column(columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(columnDefinition = "TEXT")
    private String requestParameters;

    private String clientIp;

    private String environment;

    @Builder
    public ErrorEvent(String projectId, ErrorGroup errorGroup, String exceptionType,
                      String message, String stackTrace, String requestMethod,
                      String requestUrl, String requestHeaders, String requestParameters,
                      String clientIp, String environment) {
        this.projectId = projectId;
        this.errorGroup = errorGroup;
        this.exceptionType = exceptionType;
        this.message = message;
        this.stackTrace = stackTrace;
        this.requestMethod = requestMethod;
        this.requestUrl = requestUrl;
        this.requestHeaders = requestHeaders;
        this.requestParameters = requestParameters;
        this.clientIp = clientIp;
        this.environment = environment;
    }
}
