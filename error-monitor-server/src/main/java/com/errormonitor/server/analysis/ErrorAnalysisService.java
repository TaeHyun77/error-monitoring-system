package com.errormonitor.server.analysis;

import com.errormonitor.server.analysis.dto.ErrorAnalysisResDto;
import com.errormonitor.server.analysis.repository.ErrorAnalysisRepository;
import com.errormonitor.server.error.group.ErrorGroup;
import com.errormonitor.server.error.group.repository.ErrorGroupRepository;
import com.errormonitor.server.exception.ErrorCode;
import com.errormonitor.server.exception.MonitorException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ErrorAnalysisService {

    private final ErrorAnalysisRepository analysisRepository;
    private final ErrorGroupRepository errorGroupRepository;
    private final ErrorAnalysisExecutor analysisExecutor;

    public ErrorAnalysisService(ErrorAnalysisRepository analysisRepository,
                                ErrorGroupRepository errorGroupRepository,
                                ErrorAnalysisExecutor analysisExecutor) {
        this.analysisRepository = analysisRepository;
        this.errorGroupRepository = errorGroupRepository;
        this.analysisExecutor = analysisExecutor;
    }

    @Transactional
    public void requestAnalysis(Long groupId) {
        ErrorGroup group = errorGroupRepository.findById(groupId)
                .orElseThrow(() -> new MonitorException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_GROUP_NOT_FOUND));

        Optional<ErrorAnalysis> existing = analysisRepository.findByErrorGroupId(groupId);
        if (existing.isPresent()) {
            AnalysisStatus status = existing.get().getStatus();
            if (status == AnalysisStatus.PENDING || status == AnalysisStatus.IN_PROGRESS) {
                throw new MonitorException(HttpStatus.CONFLICT, ErrorCode.ANALYSIS_IN_PROGRESS);
            }
            // If FAILED or COMPLETED, delete and retry
            analysisRepository.delete(existing.get());
            analysisRepository.flush();
        }

        ErrorAnalysis analysis = ErrorAnalysis.builder()
                .errorGroup(group)
                .build();
        analysisRepository.save(analysis);

        analysisExecutor.execute(analysis.getId(), groupId);
    }

    @Transactional(readOnly = true)
    public ErrorAnalysisResDto getAnalysis(Long groupId) {
        return analysisRepository.findByErrorGroupId(groupId)
                .map(ErrorAnalysisResDto::from)
                .orElse(null);
    }
}
