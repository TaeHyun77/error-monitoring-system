package com.errormonitor.server.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisSolutionDto {
    private String title;
    private String description;
    private String codeExample;
    private boolean recommended;
}
