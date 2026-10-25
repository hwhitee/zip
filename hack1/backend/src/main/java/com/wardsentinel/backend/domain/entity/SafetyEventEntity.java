package com.wardsentinel.backend.domain.entity;

import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
public class SafetyEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "ward_id", nullable = false, length = 32)
    private String wardId;

    @Column(name = "triggering_result_id")
    private UUID triggeringResultId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private RiskLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SafetyEventStatus status = SafetyEventStatus.DETECTED;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "assigned_to", length = 128)
    private String assignedTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes = "[]";

    @Column(name = "auto_escalate_after_ms")
    private Integer autoEscalateAfterMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SafetyEventEntity() {}

    public SafetyEventEntity(UUID eventId, UUID patientId, String wardId, UUID triggeringResultId, RiskLevel severity, SafetyEventStatus status, Integer autoEscalateAfterMs) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.patientId = patientId;
        this.wardId = wardId;
        this.triggeringResultId = triggeringResultId;
        this.severity = severity;
        this.status = status != null ? status : SafetyEventStatus.DETECTED;
        this.detectedAt = Instant.now();
        this.notes = "[]";
        this.autoEscalateAfterMs = autoEscalateAfterMs;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public UUID getTriggeringResultId() { return triggeringResultId; }
    public void setTriggeringResultId(UUID triggeringResultId) { this.triggeringResultId = triggeringResultId; }

    public RiskLevel getSeverity() { return severity; }
    public void setSeverity(RiskLevel severity) { this.severity = severity; }

    public SafetyEventStatus getStatus() { return status; }
    public void setStatus(SafetyEventStatus status) { this.status = status; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getAutoEscalateAfterMs() { return autoEscalateAfterMs; }
    public void setAutoEscalateAfterMs(Integer autoEscalateAfterMs) { this.autoEscalateAfterMs = autoEscalateAfterMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
