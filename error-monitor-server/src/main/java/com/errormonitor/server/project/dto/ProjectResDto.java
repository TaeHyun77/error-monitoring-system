package com.errormonitor.server.project.dto;

import com.errormonitor.server.project.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ProjectResDto {
    private Long id;
    private String projectId;
    private String apiKey;
    private String name;
    private String description;
    private String githubRepo;
    private LocalDateTime createdAt;

    public static ProjectResDto from(Project project) {
        return ProjectResDto.builder()
                .id(project.getId())
                .projectId(project.getProjectId())
                .apiKey(project.getApiKey())
                .name(project.getName())
                .description(project.getDescription())
                .githubRepo(project.getGithubRepo())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
