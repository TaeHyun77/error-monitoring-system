package com.errormonitor.sdk.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {
    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> parameters;
    private String clientIp;
}
