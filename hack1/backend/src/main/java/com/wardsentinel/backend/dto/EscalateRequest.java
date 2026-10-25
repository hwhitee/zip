package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class EscalateRequest {

    @NotBlank
    @JsonProperty("note")
    private String note;

    @JsonProperty("escalatedTo")
    private String escalatedTo;

    public EscalateRequest() {}

    public EscalateRequest(String note, String escalatedTo) {
        this.note = note;
        this.escalatedTo = escalatedTo;
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getEscalatedTo() { return escalatedTo; }
    public void setEscalatedTo(String escalatedTo) { this.escalatedTo = escalatedTo; }
}
