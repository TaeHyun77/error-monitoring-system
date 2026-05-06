package com.errormonitor.server.analysis.dto;

import com.errormonitor.server.analysis.AnalysisStatus;
import com.errormonitor.server.analysis.ErrorAnalysis;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ErrorAnalysisResDto {
    private Long id;
    private Long errorGroupId;
    private AnalysisStatus status;
    private String rootCause;
    private List<AnalysisSolutionDto> solutions;
    private String errorMessage;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ErrorAnalysisResDto from(ErrorAnalysis analysis) {
        List<AnalysisSolutionDto> solutionList = Collections.emptyList();
        if (analysis.getSolutions() != null && !analysis.getSolutions().isBlank()) {
            try {
                solutionList = mapper.readValue(analysis.getSolutions(),
                        new TypeReference<List<AnalysisSolutionDto>>() {});
            } catch (Exception ignored) {
            }
        }

        return ErrorAnalysisResDto.builder()
                .id(analysis.getId())
                .errorGroupId(analysis.getErrorGroup().getId())
                .status(analysis.getStatus())
                .rootCause(analysis.getRootCause())
                .solutions(solutionList)
                .errorMessage(analysis.getErrorMessage())
                .completedAt(analysis.getCompletedAt())
                .createdAt(analysis.getCreatedAt())
                .build();
    }
}
