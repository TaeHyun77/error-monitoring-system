package com.errormonitor.server.project;

import com.errormonitor.server.project.dto.ProjectReqDto;
import com.errormonitor.server.project.dto.ProjectResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResDto createProject(@RequestBody ProjectReqDto reqDto) {
        return projectService.createProject(reqDto);
    }

    @GetMapping
    public List<ProjectResDto> getProjects() {
        return projectService.getProjects();
    }
}
