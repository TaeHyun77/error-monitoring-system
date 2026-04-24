package com.errormonitor.server.error.event.repository;

import com.errormonitor.server.error.event.ErrorEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorEventRepository extends JpaRepository<ErrorEvent, Long> {
    List<ErrorEvent> findByErrorGroupIdOrderByCreatedAtDesc(Long errorGroupId);
    List<ErrorEvent> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
