package com.errormonitor.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class RequestContext {
    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> parameters;
    private String clientIp;
}
