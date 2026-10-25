package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AssignRequest {

    @NotNull
    @JsonProperty("staffId")
    private UUID staffId;

    @JsonProperty("assignedBy")
    private String assignedBy;

    public AssignRequest() {}

    public AssignRequest(UUID staffId, String assignedBy) {
        this.staffId = staffId;
        this.assignedBy = assignedBy;
    }

    public UUID getStaffId() { return staffId; }
    public void setStaffId(UUID staffId) { this.staffId = staffId; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
}
