package com.errormonitor.server.error.ingest.dto;

import lombok.Getter;

import java.util.Map;

@Getter
public class RequestContextDto {
    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> parameters;
    private String clientIp;
}
