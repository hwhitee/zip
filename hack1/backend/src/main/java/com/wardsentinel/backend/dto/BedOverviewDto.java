package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wardsentinel.backend.domain.model.BedStatus;
import com.wardsentinel.backend.domain.model.RiskLevel;
import java.util.UUID;

public class BedOverviewDto {

    @JsonProperty("bedId")
    private UUID bedId;

    @JsonProperty("wardId")
    private String wardId;

    @JsonProperty("bedLabel")
    private String bedLabel;

    @JsonProperty("status")
    private BedStatus status;

    @JsonProperty("currentPatientId")
    private UUID currentPatientId;

    @JsonProperty("patientDisplayName")
    private String patientDisplayName;

    @JsonProperty("latestRiskScore")
    private Double latestRiskScore;

    @JsonProperty("latestRiskLevel")
    private RiskLevel latestRiskLevel;

    @JsonProperty("activeEventId")
    private UUID activeEventId;

    @JsonProperty("activeEventSeverity")
    private RiskLevel activeEventSeverity;

    @JsonProperty("assignedNurseName")
    private String assignedNurseName;

    @JsonProperty("xCoord")
    private Double xCoord;

    @JsonProperty("yCoord")
    private Double yCoord;

    public BedOverviewDto() {}

    public UUID getBedId() { return bedId; }
    public void setBedId(UUID bedId) { this.bedId = bedId; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public String getBedLabel() { return bedLabel; }
    public void setBedLabel(String bedLabel) { this.bedLabel = bedLabel; }

    public BedStatus getStatus() { return status; }
    public void setStatus(BedStatus status) { this.status = status; }

    public UUID getCurrentPatientId() { return currentPatientId; }
    public void setCurrentPatientId(UUID currentPatientId) { this.currentPatientId = currentPatientId; }

    public String getPatientDisplayName() { return patientDisplayName; }
    public void setPatientDisplayName(String patientDisplayName) { this.patientDisplayName = patientDisplayName; }

    public Double getLatestRiskScore() { return latestRiskScore; }
    public void setLatestRiskScore(Double latestRiskScore) { this.latestRiskScore = latestRiskScore; }

    public RiskLevel getLatestRiskLevel() { return latestRiskLevel; }
    public void setLatestRiskLevel(RiskLevel latestRiskLevel) { this.latestRiskLevel = latestRiskLevel; }

    public UUID getActiveEventId() { return activeEventId; }
    public void setActiveEventId(UUID activeEventId) { this.activeEventId = activeEventId; }

    public RiskLevel getActiveEventSeverity() { return activeEventSeverity; }
    public void setActiveEventSeverity(RiskLevel activeEventSeverity) { this.activeEventSeverity = activeEventSeverity; }

    public String getAssignedNurseName() { return assignedNurseName; }
    public void setAssignedNurseName(String assignedNurseName) { this.assignedNurseName = assignedNurseName; }

    public Double getXCoord() { return xCoord; }
    public void setXCoord(Double xCoord) { this.xCoord = xCoord; }

    public Double getYCoord() { return yCoord; }
    public void setYCoord(Double yCoord) { this.yCoord = yCoord; }
}
