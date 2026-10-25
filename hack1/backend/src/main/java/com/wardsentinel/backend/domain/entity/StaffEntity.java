package com.wardsentinel.backend.domain.entity;

import com.wardsentinel.backend.domain.model.StaffRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff")
public class StaffEntity {

    @Id
    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private StaffRole role = StaffRole.STAFF_NURSE;

    @Column(name = "on_shift", nullable = false)
    private Boolean onShift = true;

    @Column(name = "available", nullable = false)
    private Boolean available = true;

    @Column(name = "primary_ward_id", nullable = false, length = 32)
    private String primaryWardId;

    @Column(name = "eligible_wards", nullable = false, length = 128)
    private String eligibleWards = "WARD-4B";

    @Column(name = "active_assignments_count", nullable = false)
    private Integer activeAssignmentsCount = 0;

    @Column(name = "current_x", nullable = false)
    private Double currentX = 0.0;

    @Column(name = "current_y", nullable = false)
    private Double currentY = 0.0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public StaffEntity() {}

    public StaffEntity(UUID staffId, String name, StaffRole role, Boolean onShift, Boolean available, String primaryWardId, String eligibleWards, Double currentX, Double currentY) {
        this.staffId = staffId != null ? staffId : UUID.randomUUID();
        this.name = name;
        this.role = role;
        this.onShift = onShift;
        this.available = available;
        this.primaryWardId = primaryWardId;
        this.eligibleWards = eligibleWards;
        this.activeAssignmentsCount = 0;
        this.currentX = currentX;
        this.currentY = currentY;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isEligibleForWard(String wardId) {
        if (wardId == null) return false;
        if (wardId.equalsIgnoreCase(primaryWardId)) return true;
        String[] parts = eligibleWards.split(",");
        for (String p : parts) {
            if (p.trim().equalsIgnoreCase(wardId)) return true;
        }
        return false;
    }

    public UUID getStaffId() { return staffId; }
    public void setStaffId(UUID staffId) { this.staffId = staffId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public StaffRole getRole() { return role; }
    public void setRole(StaffRole role) { this.role = role; }

    public Boolean getOnShift() { return onShift; }
    public void setOnShift(Boolean onShift) { this.onShift = onShift; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public String getPrimaryWardId() { return primaryWardId; }
    public void setPrimaryWardId(String primaryWardId) { this.primaryWardId = primaryWardId; }

    public String getEligibleWards() { return eligibleWards; }
    public void setEligibleWards(String eligibleWards) { this.eligibleWards = eligibleWards; }

    public Integer getActiveAssignmentsCount() { return activeAssignmentsCount; }
    public void setActiveAssignmentsCount(Integer activeAssignmentsCount) { this.activeAssignmentsCount = activeAssignmentsCount; }

    public Double getCurrentX() { return currentX; }
    public void setCurrentX(Double currentX) { this.currentX = currentX; }

    public Double getCurrentY() { return currentY; }
    public void setCurrentY(Double currentY) { this.currentY = currentY; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
