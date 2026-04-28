package com.errormonitor.server.project.dto;

import lombok.Getter;

@Getter
public class ProjectReqDto {
    private String projectId;
    private String name;
    private String description;
    private String githubRepo;
}
