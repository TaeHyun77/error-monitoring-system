package com.errormonitor.server.dashboard;

import com.errormonitor.server.error.event.ErrorEventService;
import com.errormonitor.server.error.group.ErrorGroupService;
import com.errormonitor.server.project.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class DashboardController {

    private final ProjectService projectService;
    private final ErrorGroupService errorGroupService;
    private final ErrorEventService errorEventService;

    public DashboardController(ProjectService projectService,
                               ErrorGroupService errorGroupService,
                               ErrorEventService errorEventService) {
        this.projectService = projectService;
        this.errorGroupService = errorGroupService;
        this.errorEventService = errorEventService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("projects", projectService.getProjects());
        return "dashboard";
    }

    @GetMapping("/dashboard/{projectId}")
    public String errorGroups(@PathVariable String projectId, Model model) {
        model.addAttribute("projectId", projectId);
        model.addAttribute("errorGroups", errorGroupService.getErrorGroups(projectId));
        return "error-groups";
    }

    @GetMapping("/dashboard/groups/{groupId}")
    public String errorGroupDetail(@PathVariable Long groupId, Model model) {
        model.addAttribute("group", errorGroupService.getErrorGroup(groupId));
        model.addAttribute("events", errorEventService.getEventsByGroup(groupId));
        return "error-group-detail";
    }

    @GetMapping("/dashboard/events/{eventId}")
    public String errorEventDetail(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", errorEventService.getEvent(eventId));
        return "error-event-detail";
    }

    @PostMapping("/dashboard/groups/{groupId}/resolve")
    public String resolveGroup(@PathVariable Long groupId) {
        errorGroupService.resolveErrorGroup(groupId);
        return "redirect:/dashboard/groups/" + groupId;
    }

    @PostMapping("/dashboard/groups/{groupId}/ignore")
    public String ignoreGroup(@PathVariable Long groupId) {
        errorGroupService.ignoreErrorGroup(groupId);
        return "redirect:/dashboard/groups/" + groupId;
    }
}
