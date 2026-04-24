package com.errormonitor.server.error.event;

import com.errormonitor.server.error.event.dto.ErrorEventResDto;
import com.errormonitor.server.error.event.repository.ErrorEventRepository;
import com.errormonitor.server.exception.ErrorCode;
import com.errormonitor.server.exception.MonitorException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public ErrorEventResDto getEvent(Long eventId) {
        ErrorEvent event = errorEventRepository.findById(eventId)
                .orElseThrow(() -> new MonitorException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_EVENT_NOT_FOUND));
        return ErrorEventResDto.from(event);
    }
}
