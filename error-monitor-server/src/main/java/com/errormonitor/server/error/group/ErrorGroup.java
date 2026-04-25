package com.errormonitor.server.error.group;

import com.errormonitor.server.common.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "error_groups", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"projectId", "fingerprint"})
})
public class ErrorGroup extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String fingerprint;

    @Column(nullable = false)
    private String exceptionType;

    @Column(length = 1000)
    private String message;

    private String sourceClass;

    private String sourceMethod;

    private Integer sourceLineNumber;

    @Column(nullable = false)
    private long eventCount;

    @Column(nullable = false)
    private LocalDateTime firstSeen;

    @Column(nullable = false)
    private LocalDateTime lastSeen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ErrorGroupStatus status;

    @Builder
    public ErrorGroup(String projectId, String fingerprint, String exceptionType,
                      String message, String sourceClass, String sourceMethod,
                      Integer sourceLineNumber, LocalDateTime firstSeen) {
        this.projectId = projectId;
        this.fingerprint = fingerprint;
        this.exceptionType = exceptionType;
        this.message = message;
        this.sourceClass = sourceClass;
        this.sourceMethod = sourceMethod;
        this.sourceLineNumber = sourceLineNumber;
        this.eventCount = 1;
        this.firstSeen = firstSeen;
        this.lastSeen = firstSeen;
        this.status = ErrorGroupStatus.UNRESOLVED;
    }

    public void incrementAndUpdate(LocalDateTime occurredAt) {
        this.eventCount++;
        this.lastSeen = occurredAt;
        if (this.status == ErrorGroupStatus.RESOLVED) {
            this.status = ErrorGroupStatus.REGRESSED;
        }
    }

    public void resolve() {
        this.status = ErrorGroupStatus.RESOLVED;
    }

    public void ignore() {
        this.status = ErrorGroupStatus.IGNORED;
    }
}
