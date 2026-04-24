package com.errormonitor.server.error.ingest;

import com.errormonitor.server.error.ingest.dto.ErrorIngestReqDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/errors")
public class ErrorIngestController {

    private final ErrorIngestService errorIngestService;

    public ErrorIngestController(ErrorIngestService errorIngestService) {
        this.errorIngestService = errorIngestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingestError(@RequestBody ErrorIngestReqDto reqDto) {
        errorIngestService.ingest(reqDto);
    }
}
