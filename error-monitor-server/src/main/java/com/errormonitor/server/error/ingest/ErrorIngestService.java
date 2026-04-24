package com.errormonitor.server.error.ingest;

import com.errormonitor.server.error.event.ErrorEvent;
import com.errormonitor.server.error.event.repository.ErrorEventRepository;
import com.errormonitor.server.error.group.ErrorGroup;
import com.errormonitor.server.error.group.repository.ErrorGroupRepository;
import com.errormonitor.server.error.ingest.dto.ErrorIngestReqDto;
import com.errormonitor.server.error.ingest.dto.RequestContextDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class ErrorIngestService {

    private static final Logger log = LoggerFactory.getLogger(ErrorIngestService.class);

    private final ErrorGroupRepository errorGroupRepository;
    private final ErrorEventRepository errorEventRepository;
    private final ObjectMapper objectMapper;

    public ErrorIngestService(ErrorGroupRepository errorGroupRepository,
                              ErrorEventRepository errorEventRepository,
                              ObjectMapper objectMapper) {
        this.errorGroupRepository = errorGroupRepository;
        this.errorEventRepository = errorEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ingest(ErrorIngestReqDto reqDto) {
        LocalDateTime occurredAt = LocalDateTime.ofInstant(reqDto.getTimestamp(), ZoneId.systemDefault());

        Optional<ErrorGroup> existingGroup = errorGroupRepository
                .findByProjectIdAndFingerprint(reqDto.getProjectId(), reqDto.getFingerprint());

        ErrorGroup group;
        if (existingGroup.isPresent()) {
            group = existingGroup.get();
            group.incrementAndUpdate(occurredAt);
        } else {
            group = ErrorGroup.builder()
                    .projectId(reqDto.getProjectId())
                    .fingerprint(reqDto.getFingerprint())
                    .exceptionType(reqDto.getExceptionInfo().getType())
                    .message(truncateMessage(reqDto.getExceptionInfo().getMessage()))
                    .firstSeen(occurredAt)
                    .build();
            group = errorGroupRepository.save(group);
        }

        ErrorEvent event = buildErrorEvent(reqDto, group);
        errorEventRepository.save(event);

        log.info("에러 이벤트 수신 완료 - projectId: {}, fingerprint: {}, groupEventCount: {}",
                reqDto.getProjectId(), reqDto.getFingerprint(), group.getEventCount());
    }

    private ErrorEvent buildErrorEvent(ErrorIngestReqDto reqDto, ErrorGroup group) {
        ErrorEvent.ErrorEventBuilder builder = ErrorEvent.builder()
                .projectId(reqDto.getProjectId())
                .errorGroup(group)
                .exceptionType(reqDto.getExceptionInfo().getType())
                .message(truncateMessage(reqDto.getExceptionInfo().getMessage()))
                .stackTrace(reqDto.getExceptionInfo().getRawStackTrace())
                .environment(reqDto.getEnvironment());

        RequestContextDto ctx = reqDto.getRequestContext();
        if (ctx != null) {
            builder.requestMethod(ctx.getMethod())
                    .requestUrl(ctx.getUrl())
                    .requestHeaders(toJson(ctx.getHeaders()))
                    .requestParameters(toJson(ctx.getParameters()))
                    .clientIp(ctx.getClientIp());
        }

        return builder.build();
    }

    private String truncateMessage(String message) {
        if (message == null) return null;
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 직렬화 실패", e);
            return null;
        }
    }
}
