package com.wardsentinel.backend.controller;

import com.wardsentinel.backend.domain.entity.InferenceResultEntity;
import com.wardsentinel.backend.domain.entity.SafetyEventEntity;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import com.wardsentinel.backend.domain.repository.InferenceResultRepository;
import com.wardsentinel.backend.domain.repository.SafetyEventRepository;
import com.wardsentinel.backend.dto.AcknowledgeRequest;
import com.wardsentinel.backend.dto.AssignRequest;
import com.wardsentinel.backend.dto.EscalateRequest;
import com.wardsentinel.backend.dto.ResolveRequest;
import com.wardsentinel.backend.service.SafetyEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class SafetyEventController {

    private final SafetyEventRepository safetyEventRepository;
    private final InferenceResultRepository inferenceResultRepository;
    private final SafetyEventService safetyEventService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public SafetyEventController(
            SafetyEventRepository safetyEventRepository,
            InferenceResultRepository inferenceResultRepository,
            SafetyEventService safetyEventService,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        this.safetyEventRepository = safetyEventRepository;
        this.inferenceResultRepository = inferenceResultRepository;
        this.safetyEventService = safetyEventService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{id}/explanation")
    public com.wardsentinel.backend.dto.StructuredExplanationDto getEventExplanation(@PathVariable UUID id) {
        SafetyEventEntity event = getEvent(id);
        if (event.getTriggeringResultId() == null) {
            return com.wardsentinel.backend.dto.StructuredExplanationDto.neutralFallback(java.util.List.of());
        }
        java.util.Optional<InferenceResultEntity> resOpt = inferenceResultRepository.findById(event.getTriggeringResultId());
        if (resOpt.isEmpty() || resOpt.get().getGeminiExplanation() == null || resOpt.get().getGeminiExplanation().isBlank()) {
            return com.wardsentinel.backend.dto.StructuredExplanationDto.neutralFallback(java.util.List.of());
        }

        try {
            return objectMapper.readValue(resOpt.get().getGeminiExplanation(), com.wardsentinel.backend.dto.StructuredExplanationDto.class);
        } catch (Exception ex) {
            String text = resOpt.get().getGeminiExplanation();
            com.wardsentinel.backend.dto.StructuredExplanationDto fallback = com.wardsentinel.backend.dto.StructuredExplanationDto.neutralFallback(java.util.List.of());
            fallback.setSummary(text);
            return fallback;
        }
    }

    @GetMapping
    public List<SafetyEventEntity> listEvents(
            @RequestParam(required = false) String wardId,
            @RequestParam(required = false) SafetyEventStatus status
    ) {
        if (wardId != null && status != null) {
            return safetyEventRepository.findByWardIdAndStatusInOrderByDetectedAtDesc(wardId, List.of(status));
        } else if (wardId != null) {
            return safetyEventRepository.findByWardIdOrderByDetectedAtDesc(wardId);
        }
        return safetyEventRepository.findAll();
    }

    @GetMapping("/{id}")
    public SafetyEventEntity getEvent(@PathVariable UUID id) {
        return safetyEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + id));
    }

    @GetMapping("/{id}/evidence")
    public InferenceResultEntity getEventEvidence(@PathVariable UUID id) {
        SafetyEventEntity event = getEvent(id);
        if (event.getTriggeringResultId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No triggering inference result linked to event " + id);
        }
        return inferenceResultRepository.findById(event.getTriggeringResultId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inference result not found: " + event.getTriggeringResultId()));
    }

    @PostMapping("/{id}/acknowledge")
    public SafetyEventEntity acknowledgeEvent(@PathVariable UUID id, @Valid @RequestBody AcknowledgeRequest req) {
        return safetyEventService.acknowledgeEvent(id, req.getAssignedTo(), req.getNote());
    }

    @PostMapping("/{id}/investigate")
    public SafetyEventEntity investigateEvent(@PathVariable UUID id, @RequestParam(required = false) String author, @RequestParam String note) {
        return safetyEventService.investigateEvent(id, author, note);
    }

    @PostMapping("/{id}/escalate")
    public SafetyEventEntity escalateEvent(@PathVariable UUID id, @Valid @RequestBody EscalateRequest req) {
        return safetyEventService.escalateEvent(id, "Staff", req.getNote(), req.getEscalatedTo());
    }

    @PostMapping("/{id}/resolve")
    public SafetyEventEntity resolveEvent(@PathVariable UUID id, @Valid @RequestBody ResolveRequest req) {
        return safetyEventService.resolveEvent(id, req.getResolvedBy(), req.getNote());
    }

    @PostMapping("/{id}/dismiss")
    public SafetyEventEntity dismissEvent(@PathVariable UUID id, @RequestParam(required = false) String clinician, @RequestParam String reason) {
        return safetyEventService.dismissEvent(id, clinician, reason);
    }

    @PostMapping("/{id}/assign")
    public SafetyEventEntity assignEvent(@PathVariable UUID id, @Valid @RequestBody AssignRequest req) {
        return safetyEventService.assignNurse(id, req.getStaffId().toString(), req.getAssignedBy());
    }
}
