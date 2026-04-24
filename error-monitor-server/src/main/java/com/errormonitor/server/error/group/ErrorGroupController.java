package com.errormonitor.server.error.group;

import com.errormonitor.server.error.group.dto.ErrorGroupResDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ErrorGroupController {

    private final ErrorGroupService errorGroupService;

    public ErrorGroupController(ErrorGroupService errorGroupService) {
        this.errorGroupService = errorGroupService;
    }

    @GetMapping("/projects/{projectId}/error-groups")
    public List<ErrorGroupResDto> getErrorGroups(@PathVariable String projectId) {
        return errorGroupService.getErrorGroups(projectId);
    }

    @GetMapping("/error-groups/{groupId}")
    public ErrorGroupResDto getErrorGroup(@PathVariable Long groupId) {
        return errorGroupService.getErrorGroup(groupId);
    }

    @PatchMapping("/error-groups/{groupId}/resolve")
    public ResponseEntity<Void> resolveErrorGroup(@PathVariable Long groupId) {
        errorGroupService.resolveErrorGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
