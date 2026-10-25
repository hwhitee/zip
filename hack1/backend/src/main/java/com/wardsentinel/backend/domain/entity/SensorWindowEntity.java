package com.wardsentinel.backend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sensor_windows")
public class SensorWindowEntity {

    @Id
    @Column(name = "window_id")
    private UUID windowId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "start_epoch", nullable = false)
    private Instant startEpoch;

    @Column(name = "end_epoch", nullable = false)
    private Instant endEpoch;

    @Column(name = "sampling_hz", nullable = false)
    private Double samplingHz = 1.0;

    @Column(name = "signal_quality", nullable = false, length = 16)
    private String signalQuality = "GOOD";

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "firmware_ver", length = 32)
    private String firmwareVer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public SensorWindowEntity() {}

    public SensorWindowEntity(UUID windowId, UUID patientId, Instant startEpoch, Instant endEpoch, Double samplingHz, String signalQuality, String payload, String deviceId, String firmwareVer) {
        this.windowId = windowId;
        this.patientId = patientId;
        this.startEpoch = startEpoch;
        this.endEpoch = endEpoch;
        this.samplingHz = samplingHz;
        this.signalQuality = signalQuality;
        this.payload = payload;
        this.deviceId = deviceId;
        this.firmwareVer = firmwareVer;
        this.createdAt = Instant.now();
    }

    public UUID getWindowId() { return windowId; }
    public void setWindowId(UUID windowId) { this.windowId = windowId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public Instant getStartEpoch() { return startEpoch; }
    public void setStartEpoch(Instant startEpoch) { this.startEpoch = startEpoch; }

    public Instant getEndEpoch() { return endEpoch; }
    public void setEndEpoch(Instant endEpoch) { this.endEpoch = endEpoch; }

    public Double getSamplingHz() { return samplingHz; }
    public void setSamplingHz(Double samplingHz) { this.samplingHz = samplingHz; }

    public String getSignalQuality() { return signalQuality; }
    public void setSignalQuality(String signalQuality) { this.signalQuality = signalQuality; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFirmwareVer() { return firmwareVer; }
    public void setFirmwareVer(String firmwareVer) { this.firmwareVer = firmwareVer; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
