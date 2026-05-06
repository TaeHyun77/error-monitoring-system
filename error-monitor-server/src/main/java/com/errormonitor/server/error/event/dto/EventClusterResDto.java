package com.errormonitor.server.error.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EventClusterResDto {
    private String message;
    private Integer sourceLineNumber;
    private long count;
    private LocalDateTime latestAt;
    private ErrorEventResDto sampleEvent;
}
