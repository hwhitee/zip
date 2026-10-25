package com.wardsentinel.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardsentinel.backend.domain.entity.*;
import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import com.wardsentinel.backend.domain.repository.*;
import com.wardsentinel.backend.dto.InferenceResponseDto;
import com.wardsentinel.backend.dto.TelemetryWindowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelemetryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);

    private final SensorWindowRepository sensorWindowRepository;
    private final InferenceResultRepository inferenceResultRepository;
    private final SafetyEventRepository safetyEventRepository;
    private final BedRepository bedRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final InferenceClientService inferenceClientService;
    private final SafetyPolicyEngine safetyPolicyEngine;
    private final NurseDispatchService nurseDispatchService;
    private final PrivacyLedgerService privacyLedgerService;
    private final ObjectMapper objectMapper;

    public TelemetryIngestionService(
            SensorWindowRepository sensorWindowRepository,
            InferenceResultRepository inferenceResultRepository,
            SafetyEventRepository safetyEventRepository,
            BedRepository bedRepository,
            OutboxEventRepository outboxEventRepository,
            InferenceClientService inferenceClientService,
            SafetyPolicyEngine safetyPolicyEngine,
            NurseDispatchService nurseDispatchService,
            PrivacyLedgerService privacyLedgerService,
            ObjectMapper objectMapper
    ) {
        this.sensorWindowRepository = sensorWindowRepository;
        this.inferenceResultRepository = inferenceResultRepository;
        this.safetyEventRepository = safetyEventRepository;
        this.bedRepository = bedRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.inferenceClientService = inferenceClientService;
        this.safetyPolicyEngine = safetyPolicyEngine;
        this.nurseDispatchService = nurseDispatchService;
        this.privacyLedgerService = privacyLedgerService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InferenceResponseDto ingestTelemetryWindow(TelemetryWindowRequest request) {
        // 1. Validate required fields & timestamp ordering
        if (!request.isTimestampOrderingValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endEpochMs must be strictly greater than startEpochMs");
        }
        if (!request.isDurationValid(300_000L)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Window duration must be exactly 300,000 ms (5 minutes)");
        }

        // 2. Idempotency Check: if window was already processed, return existing result without duplicate alerts
        Optional<SensorWindowEntity> existingWindow = sensorWindowRepository.findById(request.getWindowId());
        if (existingWindow.isPresent()) {
            Optional<InferenceResultEntity> existingResult = inferenceResultRepository.findByWindowId(request.getWindowId());
            if (existingResult.isPresent()) {
                log.info("Idempotent replay: Window {} already processed. Returning cached result.", request.getWindowId());
                return mapEntityToDto(existingResult.get());
            }
        }

        // 3. Call Python Inference Service (with timeout & fallback)
        Optional<InferenceResponseDto> inferenceOpt = inferenceClientService.callPredict(request);
        InferenceResponseDto inference;

        if (inferenceOpt.isPresent()) {
            inference = inferenceOpt.get();
        } else {
            // RELIABILITY RULE: Never fabricate a usable risk score when inference fails!
            // Represent inference failure explicitly as a degraded-system condition.
            log.warn("Inference service unavailable for window {}. Flagging degraded operational condition without score fabrication.", request.getWindowId());
            inference = new InferenceResponseDto();
            inference.setResultId(UUID.randomUUID());
            inference.setWindowId(request.getWindowId());
            inference.setPatientId(request.getPatientId());
            inference.setComputedAtMs(System.currentTimeMillis());
            inference.setLatencyMs(0);
            inference.setRiskScore(null); // Never invent a usable risk score on failure
            inference.setRiskLevel(RiskLevel.HIGH); // Degraded fail-safe triage level
            inference.setUncertainty(1.0);
            inference.setMissingModalities(List.of("INFERENCE_SERVICE_UNAVAILABLE"));
            inference.setWarnings(List.of("Inference service unavailable; risk score NOT fabricated. Degraded operational safety review required."));
            inference.setModelVersion("fallback-service-unavailable");
            inference.setModelType("SKIPPED");
        }

        // 4. Persist SensorWindowEntity (raw audio/video is strictly rejected and never saved)
        Instant startInstant = Instant.ofEpochMilli(request.getStartEpochMs());
        Instant endInstant = Instant.ofEpochMilli(request.getEndEpochMs());
        String signalQuality = (request.getMetadata() != null && request.getMetadata().getSignalQuality() != null)
                ? request.getMetadata().getSignalQuality().name()
                : "GOOD";

        SensorWindowEntity windowEntity = new SensorWindowEntity(
                request.getWindowId(),
                request.getPatientId(),
                startInstant,
                endInstant,
                request.getSamplingHz(),
                signalQuality,
                "{\"summary\":\"Derived feature window: zero raw audio/video collected\"}",
                request.getMetadata() != null ? request.getMetadata().getDeviceId() : "UNKNOWN",
                request.getMetadata() != null ? request.getMetadata().getFirmwareVer() : "UNKNOWN"
        );
        sensorWindowRepository.save(windowEntity);

        // 5. Persist InferenceResultEntity
        String contributingFactorsJson = "[]";
        try {
            if (inference.getContributingFactors() != null) {
                contributingFactorsJson = objectMapper.writeValueAsString(inference.getContributingFactors());
            }
        } catch (Exception ignored) {}

        InferenceResultEntity resultEntity = new InferenceResultEntity(
                inference.getResultId(),
                request.getWindowId(),
                request.getPatientId(),
                Instant.ofEpochMilli(inference.getComputedAtMs() != null ? inference.getComputedAtMs() : System.currentTimeMillis()),
                inference.getLatencyMs(),
                inference.getRiskScore(),
                inference.getRiskLevel(),
                contributingFactorsJson,
                inference.getGeminiExplanation(),
                inference.getModelVersion(),
                inference.getModelType()
        );
        inferenceResultRepository.save(resultEntity);

        // 6. Record in Auditable Privacy Ledger
        String patientToken = "pt-" + request.getPatientId().toString().substring(0, 8);
        privacyLedgerService.recordLedgerEntry(
                request.getWindowId(),
                patientToken,
                "WINDOW_INGESTION_AND_INFERENCE",
                "Ingested 5-minute ambient window. Synthetic vitals evaluated. Zero raw audio/video/biometric imagery stored."
        );

        // 7. Apply Deterministic Safety Policy (Java-governed, not LLM)
        SafetyPolicyEngine.PolicyDecision policyDecision = safetyPolicyEngine.evaluate(
                inference.getRiskScore(),
                (request.getMetadata() != null) ? request.getMetadata().getSignalQuality() : null,
                inference.getUncertainty(),
                inference.getModelType()
        );

        // 8. Event Creation & Dispatching (Idempotent per active event)
        if (policyDecision.requiresSafetyEvent()) {
            handleSafetyEventCreation(request, resultEntity, policyDecision);
        }

        // 9. Publish Outbox Events for Real-Time WebSocket Delivery
        publishOutboxRiskUpdate(request.getWardId(), request.getPatientId(), inference);

        return inference;
    }

    private void handleSafetyEventCreation(
            TelemetryWindowRequest request,
            InferenceResultEntity resultEntity,
            SafetyPolicyEngine.PolicyDecision policyDecision
    ) {
        // Check if patient already has an active, unresolved alert
        List<SafetyEventEntity> activeEvents = safetyEventRepository.findByPatientIdAndStatusIn(
                request.getPatientId(),
                List.of(SafetyEventStatus.DETECTED, SafetyEventStatus.ACKNOWLEDGED, SafetyEventStatus.INVESTIGATING, SafetyEventStatus.ESCALATED)
        );

        if (!activeEvents.isEmpty()) {
            SafetyEventEntity existing = activeEvents.getFirst();
            log.info("Patient {} already has active event {}. Updating triggering result without duplicate alert.", request.getPatientId(), existing.getEventId());
            existing.setTriggeringResultId(resultEntity.getResultId());
            if (policyDecision.severity().ordinal() > existing.getSeverity().ordinal()) {
                existing.setSeverity(policyDecision.severity());
            }
            existing.setUpdatedAt(Instant.now());
            safetyEventRepository.save(existing);
            return;
        }

        // Lookup bed coordinates for proximity dispatch
        Double bedX = 0.0;
        Double bedY = 0.0;
        Optional<BedEntity> bedOpt = bedRepository.findByWardIdAndBedLabel(request.getWardId(), request.getBedLabel());
        if (bedOpt.isPresent()) {
            bedX = bedOpt.get().getXCoord();
            bedY = bedOpt.get().getYCoord();
        }

        // Run nurse dispatch engine
        NurseDispatchService.DispatchResult dispatch = nurseDispatchService.dispatchNurseForEvent(request.getWardId(), bedX, bedY);

        SafetyEventEntity event = new SafetyEventEntity(
                UUID.randomUUID(),
                request.getPatientId(),
                request.getWardId(),
                resultEntity.getResultId(),
                policyDecision.severity(),
                SafetyEventStatus.DETECTED,
                policyDecision.autoEscalateAfterMs()
        );

        if (dispatch.assignedStaff() != null) {
            event.setAssignedTo(dispatch.assignedStaff().getName());
        }
        String noteText = "SKIPPED".equalsIgnoreCase(resultEntity.getModelType())
                ? "DEGRADED_SYSTEM: Inference service unavailable. Operational safety review required. " + dispatch.dispatchNote()
                : dispatch.dispatchNote();
        event.setNotes("[{\"ts\":\"" + Instant.now() + "\",\"author\":\"DispatchEngine\",\"text\":\"" + noteText + "\"}]");

        SafetyEventEntity savedEvent = safetyEventRepository.save(event);

        // Outbox event for WebSocket alert
        try {
            String eventJson = objectMapper.writeValueAsString(savedEvent);
            OutboxEventEntity outbox = new OutboxEventEntity(
                    UUID.randomUUID(),
                    "SafetyEvent",
                    savedEvent.getEventId().toString(),
                    "SAFETY_EVENT_DETECTED",
                    "/topic/ward/" + request.getWardId() + "/alerts",
                    eventJson
            );
            outboxEventRepository.save(outbox);
        } catch (Exception ex) {
            log.error("Failed to enqueue outbox event for SafetyEvent {}", savedEvent.getEventId(), ex);
        }
    }

    private void publishOutboxRiskUpdate(String wardId, UUID patientId, InferenceResponseDto inference) {
        try {
            String payload = objectMapper.writeValueAsString(inference);
            outboxEventRepository.save(new OutboxEventEntity(
                    UUID.randomUUID(),
                    "InferenceResult",
                    inference.getResultId().toString(),
                    "RISK_SCORE_UPDATED",
                    "/topic/ward/" + wardId + "/risk",
                    payload
            ));
            outboxEventRepository.save(new OutboxEventEntity(
                    UUID.randomUUID(),
                    "InferenceResult",
                    inference.getResultId().toString(),
                    "PATIENT_RISK_UPDATED",
                    "/topic/patient/" + patientId + "/risk",
                    payload
            ));
        } catch (Exception ex) {
            log.error("Failed to enqueue outbox risk update", ex);
        }
    }

    private InferenceResponseDto mapEntityToDto(InferenceResultEntity entity) {
        InferenceResponseDto dto = new InferenceResponseDto();
        dto.setResultId(entity.getResultId());
        dto.setWindowId(entity.getWindowId());
        dto.setPatientId(entity.getPatientId());
        dto.setComputedAtMs(entity.getComputedAt().toEpochMilli());
        dto.setLatencyMs(entity.getLatencyMs());
        dto.setRiskScore(entity.getRiskScore());
        dto.setRiskLevel(entity.getRiskLevel());
        dto.setGeminiExplanation(entity.getGeminiExplanation());
        dto.setModelVersion(entity.getModelVersion());
        dto.setModelType(entity.getModelType());
        return dto;
    }
}
