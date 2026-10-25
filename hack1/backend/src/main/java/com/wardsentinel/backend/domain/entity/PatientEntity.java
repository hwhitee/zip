package com.wardsentinel.backend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class PatientEntity {

    @Id
    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "mrn", nullable = false, unique = true, length = 32)
    private String mrn;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "ward_id", nullable = false, length = 32)
    private String wardId;

    @Column(name = "bed_label", nullable = false, length = 8)
    private String bedLabel;

    @Column(name = "admitted_at", nullable = false)
    private Instant admittedAt = Instant.now();

    @Column(name = "discharged_at")
    private Instant dischargedAt;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public PatientEntity() {}

    public PatientEntity(UUID patientId, String mrn, String displayName, String wardId, String bedLabel) {
        this.patientId = patientId;
        this.mrn = mrn;
        this.displayName = displayName;
        this.wardId = wardId;
        this.bedLabel = bedLabel;
        this.admittedAt = Instant.now();
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public String getBedLabel() { return bedLabel; }
    public void setBedLabel(String bedLabel) { this.bedLabel = bedLabel; }

    public Instant getAdmittedAt() { return admittedAt; }
    public void setAdmittedAt(Instant admittedAt) { this.admittedAt = admittedAt; }

    public Instant getDischargedAt() { return dischargedAt; }
    public void setDischargedAt(Instant dischargedAt) { this.dischargedAt = dischargedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
