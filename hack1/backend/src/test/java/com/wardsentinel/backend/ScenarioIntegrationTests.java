package com.wardsentinel.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardsentinel.backend.domain.entity.BedEntity;
import com.wardsentinel.backend.domain.entity.PatientEntity;
import com.wardsentinel.backend.domain.entity.SafetyEventEntity;
import com.wardsentinel.backend.domain.entity.StaffEntity;
import com.wardsentinel.backend.domain.model.BedStatus;
import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import com.wardsentinel.backend.domain.model.SignalQuality;
import com.wardsentinel.backend.domain.model.StaffRole;
import com.wardsentinel.backend.domain.repository.*;
import com.wardsentinel.backend.dto.*;
import com.wardsentinel.backend.service.InferenceClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScenarioIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private SafetyEventRepository safetyEventRepository;

    @Autowired
    private SensorWindowRepository sensorWindowRepository;

    @Autowired
    private InferenceResultRepository inferenceResultRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private InferenceClientService inferenceClientService;

    private UUID patientId;
    private final String wardId = "WARD-4B";
    private final String bedLabel = "04";

    @BeforeEach
    void setUp() {
        safetyEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        inferenceResultRepository.deleteAll();
        sensorWindowRepository.deleteAll();
        bedRepository.deleteAll();
        patientRepository.deleteAll();
        staffRepository.deleteAll();

        patientId = UUID.randomUUID();
        String mrn = "MRN-" + UUID.randomUUID().toString().substring(0, 8);
        PatientEntity patient = new PatientEntity(patientId, mrn, "Synthetic Patient Alpha", wardId, bedLabel);
        patientRepository.save(patient);

        BedEntity bed = new BedEntity(UUID.randomUUID(), wardId, bedLabel, BedStatus.OCCUPIED, patientId, 2.0, 3.0);
        bedRepository.save(bed);

        StaffEntity staff = new StaffEntity(
                UUID.randomUUID(), "Nurse Sarah", StaffRole.STAFF_NURSE,
                true, true, wardId, wardId, 2.0, 3.0
        );
        staffRepository.save(staff);
    }

    private TelemetryWindowRequest createRequest(UUID windowId, SignalQuality signalQuality) {
        TelemetryWindowRequest req = new TelemetryWindowRequest();
        req.setWindowId(windowId);
        req.setPatientId(patientId);
        req.setWardId(wardId);
        req.setBedLabel(bedLabel);
        long now = 1700000000000L;
        req.setStartEpochMs(now);
        req.setEndEpochMs(now + 300_000L);
        req.setSamplingHz(1.0);
        req.setSequenceNumber(1L);
        req.setSchemaVersion("1.0.0");

        SensorPayloadDto sensors = new SensorPayloadDto();
        sensors.setHeartRateBpm(Collections.nCopies(300, 75.0));
        sensors.setSpo2Pct(Collections.nCopies(300, 98.0));
        sensors.setRespRateBreaths(Collections.nCopies(300, 16.0));
        sensors.setSkinTempC(Collections.nCopies(300, 34.0));
        sensors.setMotionG(Collections.nCopies(300, 0.05));
        sensors.setAmbientNoiseDb(Collections.nCopies(300, 40.0));
        sensors.setLightLux(Collections.nCopies(300, 100.0));
        sensors.setTorsoElevation(Collections.nCopies(300, 10.0));
        sensors.setEdgeProximity(Collections.nCopies(300, 0.1));
        sensors.setLowerBodyMovement(Collections.nCopies(300, 0.05));
        sensors.setBedPressureShift(Collections.nCopies(300, 0.1));
        sensors.setOccupancyConfidence(Collections.nCopies(300, 0.99));
        req.setSensors(sensors);

        req.setMetadata(new SensorMetadataDto("DEVICE-04", "1.0.0", signalQuality));
        return req;
    }

    private InferenceResponseDto createMockInference(UUID windowId, double score, RiskLevel level, String explanation) {
        InferenceResponseDto dto = new InferenceResponseDto();
        dto.setResultId(UUID.randomUUID());
        dto.setWindowId(windowId);
        dto.setPatientId(patientId);
        dto.setComputedAtMs(System.currentTimeMillis());
        dto.setLatencyMs(35);
        dto.setRiskScore(score);
        dto.setRiskLevel(level);
        dto.setUncertainty(0.05);
        dto.setModelVersion("v0.1.0-heuristic");
        dto.setModelType("HEURISTIC");
        dto.setGeminiExplanation(explanation);
        return dto;
    }

    @Test
    @DisplayName("Scenario 1: SAFE_REPOSITIONING - No alert created when deterministic policy does not qualify it")
    void testScenario1_SafeRepositioning_NoAlert() throws Exception {
        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createRequest(windowId, SignalQuality.GOOD);
        InferenceResponseDto mockInference = createMockInference(windowId, 0.20, RiskLevel.LOW, null);
        when(inferenceClientService.callPredict(any())).thenReturn(Optional.of(mockInference));

        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.risk_score").value(0.20))
                .andExpect(jsonPath("$.risk_level").value("LOW"));

        // No safety event should be created
        List<SafetyEventEntity> events = safetyEventRepository.findAll();
        assertThat(events).isEmpty();

        // Ward overview shows 0 active events
        mockMvc.perform(get("/api/v1/wards/" + wardId + "/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEventsCount").value(0));
    }

    @Test
    @DisplayName("Scenario 2: PROGRESSIVE_BED_EXIT - Evolving risk triggers event, assigns nurse, acknowledges, verifies ledger")
    void testScenario2_ProgressiveBedExit_EndToEnd() throws Exception {
        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createRequest(windowId, SignalQuality.GOOD);
        InferenceResponseDto mockInference = createMockInference(windowId, 0.88, RiskLevel.CRITICAL, "{\"summary\":\"Patient egress trajectory detected\"}");
        when(inferenceClientService.callPredict(any())).thenReturn(Optional.of(mockInference));

        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Safety event should exist with assigned nurse
        List<SafetyEventEntity> events = safetyEventRepository.findAll();
        assertThat(events).hasSize(1);
        SafetyEventEntity event = events.getFirst();
        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.DETECTED);
        assertThat(event.getSeverity()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(event.getAssignedTo()).isEqualTo("Nurse Sarah");

        // Acknowledge the event
        AcknowledgeRequest ackReq = new AcknowledgeRequest();
        ackReq.setAssignedTo("Nurse Sarah");
        ackReq.setNote("Responding at bedside");

        mockMvc.perform(post("/api/v1/events/" + event.getEventId() + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        // Verify privacy ledger
        mockMvc.perform(post("/api/v1/privacy/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    @DisplayName("Scenario 3: Duplicate Replay - Resending exact window returns cached result without duplicate alert")
    void testScenario3_DuplicateReplay_Idempotent() throws Exception {
        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createRequest(windowId, SignalQuality.GOOD);
        InferenceResponseDto mockInference = createMockInference(windowId, 0.85, RiskLevel.CRITICAL, null);
        when(inferenceClientService.callPredict(any())).thenReturn(Optional.of(mockInference));

        // First transmission
        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertThat(safetyEventRepository.findAll()).hasSize(1);
        assertThat(sensorWindowRepository.findAll()).hasSize(1);
        UUID firstResultId = inferenceResultRepository.findAll().getFirst().getResultId();

        // Second transmission (duplicate windowId replay)
        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result_id").value(firstResultId.toString()));

        // Crucial invariant: Still exactly 1 safety event, no duplicate created
        assertThat(safetyEventRepository.findAll()).hasSize(1);
        assertThat(sensorWindowRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Scenario 4: SENSOR_DROPOUT - SignalQuality LOST triggers HIGH fail-safe alert without safe score fabrication")
    void testScenario4_SensorDropout_TriggersFailsafeAlert() throws Exception {
        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createRequest(windowId, SignalQuality.LOST);
        // Even if predictor returns 0.10, policy must override on LOST signal
        InferenceResponseDto mockInference = createMockInference(windowId, 0.10, RiskLevel.LOW, null);
        when(inferenceClientService.callPredict(any())).thenReturn(Optional.of(mockInference));

        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        List<SafetyEventEntity> events = safetyEventRepository.findAll();
        assertThat(events).hasSize(1);
        SafetyEventEntity event = events.getFirst();
        // Deterministic safety policy triggers HIGH on LOST hardware signal
        assertThat(event.getSeverity()).isEqualTo(RiskLevel.HIGH);
        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.DETECTED);
    }

    @Test
    @DisplayName("Scenario 5: NURSE_UNAVAILABLE - Ineligible staff not assigned; event remains pending with escalation")
    void testScenario5_NurseUnavailable_PendingEscalation() throws Exception {
        // Mark all nurses off-shift
        staffRepository.findAll().forEach(s -> {
            s.setOnShift(false);
            s.setAvailable(false);
            staffRepository.save(s);
        });

        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createRequest(windowId, SignalQuality.GOOD);
        InferenceResponseDto mockInference = createMockInference(windowId, 0.85, RiskLevel.CRITICAL, null);
        when(inferenceClientService.callPredict(any())).thenReturn(Optional.of(mockInference));

        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        List<SafetyEventEntity> events = safetyEventRepository.findAll();
        assertThat(events).hasSize(1);
        SafetyEventEntity event = events.getFirst();
        // No ineligible nurse assigned
        assertThat(event.getAssignedTo()).isNull();
        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.DETECTED);
        assertThat(event.getNotes()).contains("ESCALATION PENDING");
    }

    @Test
    @DisplayName("Scenario 7: Gemini Unavailable - Deterministic event creation and dispatch succeed without explanation")
    void testScenario7_GeminiUnavailable_DeterministicEngineOperates() throws Exception {
        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createRequest(windowId, SignalQuality.GOOD);
        // Inference response with null explanation
        InferenceResponseDto mockInference = createMockInference(windowId, 0.85, RiskLevel.CRITICAL, null);
        when(inferenceClientService.callPredict(any())).thenReturn(Optional.of(mockInference));

        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        List<SafetyEventEntity> events = safetyEventRepository.findAll();
        assertThat(events).hasSize(1);
        SafetyEventEntity event = events.getFirst();
        assertThat(event.getAssignedTo()).isEqualTo("Nurse Sarah");

        // Explanation endpoint returns neutral fallback
        mockMvc.perform(get("/api/v1/events/" + event.getEventId() + "/explanation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.limitations").isArray());
    }

    @Test
    @DisplayName("Scenario 8: Inference Service Timeout/Failure - Explicit fallback with non-fabricated safe score and operational event")
    void testScenario8_InferenceTimeout_HandledExplicitly() throws Exception {
        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createRequest(windowId, SignalQuality.GOOD);
        // Inference service fails/times out
        when(inferenceClientService.callPredict(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.model_type").value("SKIPPED"))
                .andExpect(jsonPath("$.risk_score").doesNotExist()) // Never fabricate a safe or arbitrary score!
                .andExpect(jsonPath("$.risk_level").value("HIGH"))
                .andExpect(jsonPath("$.uncertainty").value(1.0));

        // Verified in database: inference result has SKIPPED model_type and null risk_score
        assertThat(inferenceResultRepository.findAll()).hasSize(1);
        assertThat(inferenceResultRepository.findAll().getFirst().getModelType()).isEqualTo("SKIPPED");
        assertThat(inferenceResultRepository.findAll().getFirst().getRiskScore()).isNull();

        // Crucial invariant: Operational degraded-system safety alert IS created!
        List<SafetyEventEntity> events = safetyEventRepository.findAll();
        assertThat(events).hasSize(1);
        SafetyEventEntity event = events.getFirst();
        assertThat(event.getSeverity()).isEqualTo(RiskLevel.HIGH);
        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.DETECTED);
        assertThat(event.getNotes()).contains("DEGRADED_SYSTEM");
    }
}
