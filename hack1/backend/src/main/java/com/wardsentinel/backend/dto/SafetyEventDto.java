package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import java.util.UUID;

public class SafetyEventDto {

    @JsonProperty("eventId")
    private UUID eventId;

    @JsonProperty("patientId")
    private UUID patientId;

    @JsonProperty("wardId")
    private String wardId;

    @JsonProperty("triggeringResultId")
    private UUID triggeringResultId;

    @JsonProperty("severity")
    private RiskLevel severity;

    @JsonProperty("status")
    private SafetyEventStatus status;

    @JsonProperty("detectedAtMs")
    private Long detectedAtMs;

    @JsonProperty("acknowledgedAtMs")
    private Long acknowledgedAtMs;

    @JsonProperty("resolvedAtMs")
    private Long resolvedAtMs;

    @JsonProperty("assignedTo")
    private String assignedTo;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("autoEscalateAfterMs")
    private Integer autoEscalateAfterMs;

    public SafetyEventDto() {}

    public SafetyEventDto(UUID eventId, UUID patientId, String wardId, UUID triggeringResultId, RiskLevel severity, SafetyEventStatus status, Long detectedAtMs, Long acknowledgedAtMs, Long resolvedAtMs, String assignedTo, String notes, Integer autoEscalateAfterMs) {
        this.eventId = eventId;
        this.patientId = patientId;
        this.wardId = wardId;
        this.triggeringResultId = triggeringResultId;
        this.severity = severity;
        this.status = status;
        this.detectedAtMs = detectedAtMs;
        this.acknowledgedAtMs = acknowledgedAtMs;
        this.resolvedAtMs = resolvedAtMs;
        this.assignedTo = assignedTo;
        this.notes = notes;
        this.autoEscalateAfterMs = autoEscalateAfterMs;
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

    public Long getDetectedAtMs() { return detectedAtMs; }
    public void setDetectedAtMs(Long detectedAtMs) { this.detectedAtMs = detectedAtMs; }

    public Long getAcknowledgedAtMs() { return acknowledgedAtMs; }
    public void setAcknowledgedAtMs(Long acknowledgedAtMs) { this.acknowledgedAtMs = acknowledgedAtMs; }

    public Long getResolvedAtMs() { return resolvedAtMs; }
    public void setResolvedAtMs(Long resolvedAtMs) { this.resolvedAtMs = resolvedAtMs; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getAutoEscalateAfterMs() { return autoEscalateAfterMs; }
    public void setAutoEscalateAfterMs(Integer autoEscalateAfterMs) { this.autoEscalateAfterMs = autoEscalateAfterMs; }
}
