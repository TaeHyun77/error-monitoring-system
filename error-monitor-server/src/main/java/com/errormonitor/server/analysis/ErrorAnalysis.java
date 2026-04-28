package com.errormonitor.server.analysis;

import com.errormonitor.server.common.BaseTime;
import com.errormonitor.server.error.group.ErrorGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "error_analyses")
public class ErrorAnalysis extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_group_id", unique = true, nullable = false)
    private ErrorGroup errorGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String solutions;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime completedAt;

    @Builder
    public ErrorAnalysis(ErrorGroup errorGroup) {
        this.errorGroup = errorGroup;
        this.status = AnalysisStatus.PENDING;
    }

    public void startAnalysis() {
        this.status = AnalysisStatus.IN_PROGRESS;
    }

    public void completeAnalysis(String rootCause, String solutions) {
        this.status = AnalysisStatus.COMPLETED;
        this.rootCause = rootCause;
        this.solutions = solutions;
        this.completedAt = LocalDateTime.now();
    }

    public void failAnalysis(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}
