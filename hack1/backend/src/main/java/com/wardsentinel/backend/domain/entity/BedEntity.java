package com.wardsentinel.backend.domain.entity;

import com.wardsentinel.backend.domain.model.BedStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "beds")
public class BedEntity {

    @Id
    @Column(name = "bed_id")
    private UUID bedId;

    @Column(name = "ward_id", nullable = false, length = 32)
    private String wardId;

    @Column(name = "bed_label", nullable = false, length = 8)
    private String bedLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BedStatus status = BedStatus.AVAILABLE;

    @Column(name = "current_patient_id")
    private UUID currentPatientId;

    @Column(name = "x_coord", nullable = false)
    private Double xCoord = 0.0;

    @Column(name = "y_coord", nullable = false)
    private Double yCoord = 0.0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BedEntity() {}

    public BedEntity(UUID bedId, String wardId, String bedLabel, BedStatus status, UUID currentPatientId, Double xCoord, Double yCoord) {
        this.bedId = bedId != null ? bedId : UUID.randomUUID();
        this.wardId = wardId;
        this.bedLabel = bedLabel;
        this.status = status;
        this.currentPatientId = currentPatientId;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

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

    public Double getXCoord() { return xCoord; }
    public void setXCoord(Double xCoord) { this.xCoord = xCoord; }

    public Double getYCoord() { return yCoord; }
    public void setYCoord(Double yCoord) { this.yCoord = yCoord; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
