package com.errormonitor.server.error.event;

import com.errormonitor.server.error.event.dto.ErrorEventResDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ErrorEventController {

    private final ErrorEventService errorEventService;

    public ErrorEventController(ErrorEventService errorEventService) {
        this.errorEventService = errorEventService;
    }

    @GetMapping("/error-groups/{groupId}/events")
    public List<ErrorEventResDto> getEventsByGroup(@PathVariable Long groupId) {
        return errorEventService.getEventsByGroup(groupId);
    }

    @GetMapping("/error-events/{eventId}")
    public ErrorEventResDto getEvent(@PathVariable Long eventId) {
        return errorEventService.getEvent(eventId);
    }
}
