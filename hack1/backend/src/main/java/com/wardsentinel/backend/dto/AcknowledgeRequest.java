package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class AcknowledgeRequest {

    @NotBlank
    @JsonProperty("assignedTo")
    private String assignedTo;

    @JsonProperty("note")
    private String note;

    public AcknowledgeRequest() {}

    public AcknowledgeRequest(String assignedTo, String note) {
        this.assignedTo = assignedTo;
        this.note = note;
    }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
