package com.errormonitor.server.analysis.repository;

import com.errormonitor.server.analysis.ErrorAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ErrorAnalysisRepository extends JpaRepository<ErrorAnalysis, Long> {
    Optional<ErrorAnalysis> findByErrorGroupId(Long errorGroupId);
}
