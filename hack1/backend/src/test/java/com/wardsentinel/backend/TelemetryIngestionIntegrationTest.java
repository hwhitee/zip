package com.wardsentinel.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardsentinel.backend.domain.entity.BedEntity;
import com.wardsentinel.backend.domain.entity.PatientEntity;
import com.wardsentinel.backend.domain.entity.StaffEntity;
import com.wardsentinel.backend.domain.model.BedStatus;
import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.StaffRole;
import com.wardsentinel.backend.domain.repository.BedRepository;
import com.wardsentinel.backend.domain.repository.PatientRepository;
import com.wardsentinel.backend.domain.repository.StaffRepository;
import com.wardsentinel.backend.dto.InferenceResponseDto;
import com.wardsentinel.backend.dto.SensorMetadataDto;
import com.wardsentinel.backend.dto.SensorPayloadDto;
import com.wardsentinel.backend.dto.TelemetryWindowRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryIngestionIntegrationTest {

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

    @MockBean
    private InferenceClientService inferenceClientService;

    private UUID patientId;

    @BeforeEach
    void setUp() {
        bedRepository.deleteAll();
        patientRepository.deleteAll();
        staffRepository.deleteAll();

        patientId = UUID.randomUUID();
        String mrn = "MRN-" + UUID.randomUUID().toString().substring(0, 8);
        PatientEntity patient = new PatientEntity(patientId, mrn, "Synthetic Patient A", "WARD-4B", "04");
        patientRepository.save(patient);

        BedEntity bed = new BedEntity(UUID.randomUUID(), "WARD-4B", "04", BedStatus.OCCUPIED, patientId, 2.0, 3.0);
        bedRepository.save(bed);

        StaffEntity staff = new StaffEntity(
                UUID.randomUUID(), "Nurse Sarah", StaffRole.STAFF_NURSE,
                true, true, "WARD-4B", "WARD-4B", 2.0, 3.0
        );
        staffRepository.save(staff);
    }

    private TelemetryWindowRequest createSampleRequest(UUID windowId, long startMs, long endMs) {
        TelemetryWindowRequest req = new TelemetryWindowRequest();
        req.setWindowId(windowId);
        req.setPatientId(patientId);
        req.setWardId("WARD-4B");
        req.setBedLabel("04");
        req.setStartEpochMs(startMs);
        req.setEndEpochMs(endMs);
        req.setSamplingHz(1.0);
        req.setSequenceNumber(1L);
        req.setSchemaVersion("1.0.0");

        SensorPayloadDto sensors = new SensorPayloadDto();
        List<Double> flatVitals = Collections.nCopies(300, 75.0);
        sensors.setHeartRateBpm(flatVitals);
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

        req.setMetadata(new SensorMetadataDto("DEVICE-04", "1.0.0", null));
        return req;
    }

    @Test
    @DisplayName("End-to-end ingestion creates inference record, safety event, and privacy ledger entry")
    void testTelemetryIngestionFlow() throws Exception {
        UUID windowId = UUID.randomUUID();
        TelemetryWindowRequest req = createSampleRequest(windowId, 1700000000000L, 1700000300000L);

        InferenceResponseDto mockInference = new InferenceResponseDto();
        mockInference.setResultId(UUID.randomUUID());
        mockInference.setWindowId(windowId);
        mockInference.setPatientId(patientId);
        mockInference.setComputedAtMs(System.currentTimeMillis());
        mockInference.setLatencyMs(25);
        mockInference.setRiskScore(0.85); // Critical -> triggers event
        mockInference.setRiskLevel(RiskLevel.CRITICAL);
        mockInference.setUncertainty(0.08);
        mockInference.setModelVersion("v0.1.0-heuristic");
        mockInference.setModelType("HEURISTIC");

        when(inferenceClientService.callPredict(any())).thenReturn(Optional.of(mockInference));

        // 1. Post telemetry window
        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.window_id").value(windowId.toString()))
                .andExpect(jsonPath("$.risk_score").value(0.85))
                .andExpect(jsonPath("$.risk_level").value("CRITICAL"));

        // 2. Query Ward Overview -> Check active events
        mockMvc.perform(get("/api/v1/wards/WARD-4B/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wardId").value("WARD-4B"))
                .andExpect(jsonPath("$.activeEventsCount").value(1))
                .andExpect(jsonPath("$.criticalEventsCount").value(1));

        // 3. Query Privacy Ledger Verification
        mockMvc.perform(post("/api/v1/privacy/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    @DisplayName("Mismatched window duration rejected with 400 Bad Request")
    void testInvalidDurationRejected() throws Exception {
        UUID windowId = UUID.randomUUID();
        // 100,000 ms instead of 300,000 ms
        TelemetryWindowRequest req = createSampleRequest(windowId, 1700000000000L, 1700000100000L);

        mockMvc.perform(post("/api/v1/telemetry/windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
