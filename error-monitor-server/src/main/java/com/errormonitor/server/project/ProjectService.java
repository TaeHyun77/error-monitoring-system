package com.errormonitor.server.project;

import com.errormonitor.server.exception.ErrorCode;
import com.errormonitor.server.exception.MonitorException;
import com.errormonitor.server.project.dto.ProjectReqDto;
import com.errormonitor.server.project.dto.ProjectResDto;
import com.errormonitor.server.project.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResDto createProject(ProjectReqDto reqDto) {
        if (projectRepository.existsByProjectId(reqDto.getProjectId())) {
            throw new MonitorException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_PROJECT_ID);
        }

        String apiKey = "proj_" + UUID.randomUUID().toString().replace("-", "");

        Project project = Project.builder()
                .projectId(reqDto.getProjectId())
                .apiKey(apiKey)
                .name(reqDto.getName())
                .description(reqDto.getDescription())
                .build();

        return ProjectResDto.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResDto> getProjects() {
        return projectRepository.findAll().stream()
                .map(ProjectResDto::from)
                .toList();
    }
}
