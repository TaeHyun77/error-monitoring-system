package com.errormonitor.server.error.group;

import com.errormonitor.server.error.group.dto.ErrorGroupResDto;
import com.errormonitor.server.error.group.repository.ErrorGroupRepository;
import com.errormonitor.server.exception.ErrorCode;
import com.errormonitor.server.exception.MonitorException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErrorGroupService {

    private final ErrorGroupRepository errorGroupRepository;

    public ErrorGroupService(ErrorGroupRepository errorGroupRepository) {
        this.errorGroupRepository = errorGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<ErrorGroupResDto> getErrorGroups(String projectId) {
        return errorGroupRepository.findByProjectIdOrderByLastSeenDesc(projectId).stream()
                .map(ErrorGroupResDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ErrorGroupResDto getErrorGroup(Long groupId) {
        ErrorGroup group = errorGroupRepository.findById(groupId)
                .orElseThrow(() -> new MonitorException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_GROUP_NOT_FOUND));
        return ErrorGroupResDto.from(group);
    }

    @Transactional
    public void resolveErrorGroup(Long groupId) {
        ErrorGroup group = errorGroupRepository.findById(groupId)
                .orElseThrow(() -> new MonitorException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_GROUP_NOT_FOUND));
        group.resolve();
    }

    @Transactional
    public void ignoreErrorGroup(Long groupId) {
        ErrorGroup group = errorGroupRepository.findById(groupId)
                .orElseThrow(() -> new MonitorException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_GROUP_NOT_FOUND));
        group.ignore();
    }
}
