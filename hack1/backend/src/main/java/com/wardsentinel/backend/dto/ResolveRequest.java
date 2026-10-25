package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ResolveRequest {

    @NotBlank
    @JsonProperty("note")
    private String note;

    @JsonProperty("resolvedBy")
    private String resolvedBy;

    public ResolveRequest() {}

    public ResolveRequest(String note, String resolvedBy) {
        this.note = note;
        this.resolvedBy = resolvedBy;
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}
