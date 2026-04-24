package com.errormonitor.server.project.repository;

import com.errormonitor.server.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByProjectId(String projectId);
    boolean existsByProjectId(String projectId);
    boolean existsByApiKey(String apiKey);
}
