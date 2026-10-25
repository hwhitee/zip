package com.wardsentinel.backend.controller;

import com.wardsentinel.backend.dto.InferenceResponseDto;
import com.wardsentinel.backend.dto.TelemetryWindowRequest;
import com.wardsentinel.backend.service.TelemetryIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final TelemetryIngestionService telemetryIngestionService;

    public TelemetryController(TelemetryIngestionService telemetryIngestionService) {
        this.telemetryIngestionService = telemetryIngestionService;
    }

    @PostMapping("/windows")
    public ResponseEntity<InferenceResponseDto> ingestWindow(@Valid @RequestBody TelemetryWindowRequest request) {
        InferenceResponseDto response = telemetryIngestionService.ingestTelemetryWindow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
