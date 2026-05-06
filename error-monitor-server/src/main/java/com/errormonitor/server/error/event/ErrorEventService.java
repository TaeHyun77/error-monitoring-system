package com.errormonitor.server.error.event;

import com.errormonitor.server.error.event.dto.ErrorEventResDto;
import com.errormonitor.server.error.event.dto.EventClusterResDto;
import com.errormonitor.server.error.event.repository.ErrorEventRepository;
import com.errormonitor.server.exception.ErrorCode;
import com.errormonitor.server.exception.MonitorException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ErrorEventService {

    private final ErrorEventRepository errorEventRepository;

    public ErrorEventService(ErrorEventRepository errorEventRepository) {
        this.errorEventRepository = errorEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ErrorEventResDto> getEventsByGroup(Long groupId) {
        return errorEventRepository.findByErrorGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(ErrorEventResDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventClusterResDto> getEventClustersByGroup(Long groupId) {
        List<ErrorEvent> events = errorEventRepository.findByErrorGroupIdOrderByCreatedAtDesc(groupId);

        return events.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> Objects.toString(e.getMessage(), "") + "|" + Objects.toString(e.getSourceLineNumber(), "")
                ))
                .values().stream()
                .map(group -> {
                    ErrorEvent latest = group.get(0);
                    return EventClusterResDto.builder()
                            .message(latest.getMessage())
                            .sourceLineNumber(latest.getSourceLineNumber())
                            .count(group.size())
                            .latestAt(latest.getCreatedAt())
                            .sampleEvent(ErrorEventResDto.from(latest))
                            .build();
                })
                .sorted(Comparator.comparing(EventClusterResDto::getCount).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ErrorEventResDto getEvent(Long eventId) {
        ErrorEvent event = errorEventRepository.findById(eventId)
                .orElseThrow(() -> new MonitorException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_EVENT_NOT_FOUND));
        return ErrorEventResDto.from(event);
    }
}
