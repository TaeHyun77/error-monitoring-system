package com.errormonitor.server.error.group.repository;

import com.errormonitor.server.error.group.ErrorGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErrorGroupRepository extends JpaRepository<ErrorGroup, Long> {
    Optional<ErrorGroup> findByProjectIdAndFingerprint(String projectId, String fingerprint);
    List<ErrorGroup> findByProjectIdOrderByLastSeenDesc(String projectId);
}
