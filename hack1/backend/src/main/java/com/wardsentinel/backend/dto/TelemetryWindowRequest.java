package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public class TelemetryWindowRequest {

    @NotNull
    @JsonProperty("windowId")
    private UUID windowId;

    @NotNull
    @JsonProperty("patientId")
    private UUID patientId;

    @NotBlank
    @JsonProperty("wardId")
    private String wardId;

    @NotBlank
    @JsonProperty("bedLabel")
    private String bedLabel;

    @NotNull
    @PositiveOrZero
    @JsonProperty("startEpochMs")
    private Long startEpochMs;

    @NotNull
    @Positive
    @JsonProperty("endEpochMs")
    private Long endEpochMs;

    @Positive
    @JsonProperty("samplingHz")
    private Double samplingHz = 1.0;

    @NotBlank
    @JsonProperty("schemaVersion")
    private String schemaVersion = "1.0.0";

    @NotNull
    @PositiveOrZero
    @JsonProperty("sequenceNumber")
    private Long sequenceNumber = 0L;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @NotNull
    @Valid
    @JsonProperty("sensors")
    private SensorPayloadDto sensors;

    @NotNull
    @JsonProperty("metadata")
    private SensorMetadataDto metadata = new SensorMetadataDto();

    public TelemetryWindowRequest() {}

    public boolean isTimestampOrderingValid() {
        if (startEpochMs == null || endEpochMs == null) return false;
        return endEpochMs > startEpochMs;
    }

    public boolean isDurationValid(long expectedDurationMs) {
        if (!isTimestampOrderingValid()) return false;
        return (endEpochMs - startEpochMs) == expectedDurationMs;
    }

    public String getEffectiveIdempotencyKey() {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKey;
        }
        return windowId != null ? windowId.toString() : UUID.randomUUID().toString();
    }

    public UUID getWindowId() { return windowId; }
    public void setWindowId(UUID windowId) { this.windowId = windowId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public String getBedLabel() { return bedLabel; }
    public void setBedLabel(String bedLabel) { this.bedLabel = bedLabel; }

    public Long getStartEpochMs() { return startEpochMs; }
    public void setStartEpochMs(Long startEpochMs) { this.startEpochMs = startEpochMs; }

    public Long getEndEpochMs() { return endEpochMs; }
    public void setEndEpochMs(Long endEpochMs) { this.endEpochMs = endEpochMs; }

    public Double getSamplingHz() { return samplingHz; }
    public void setSamplingHz(Double samplingHz) { this.samplingHz = samplingHz; }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public SensorPayloadDto getSensors() { return sensors; }
    public void setSensors(SensorPayloadDto sensors) { this.sensors = sensors; }

    public SensorMetadataDto getMetadata() { return metadata; }
    public void setMetadata(SensorMetadataDto metadata) { this.metadata = metadata; }
}
