package com.wardsentinel.backend.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_ledger")
public class PrivacyLedgerEntity {

    @Id
    @Column(name = "entry_id")
    private UUID entryId;

    @Column(name = "window_id", nullable = false)
    private UUID windowId;

    @Column(name = "patient_token", nullable = false, length = 64)
    private String patientToken;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "canonical_sha256", nullable = false, length = 64)
    private String canonicalSha256;

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;

    @Column(name = "data_minimization_note", nullable = false, columnDefinition = "TEXT")
    private String dataMinimizationNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public PrivacyLedgerEntity() {}

    public PrivacyLedgerEntity(UUID entryId, UUID windowId, String patientToken, String eventType, String canonicalSha256, String prevHash, String dataMinimizationNote) {
        this.entryId = entryId != null ? entryId : UUID.randomUUID();
        this.windowId = windowId;
        this.patientToken = patientToken;
        this.eventType = eventType;
        this.canonicalSha256 = canonicalSha256;
        this.prevHash = prevHash;
        this.dataMinimizationNote = dataMinimizationNote;
        this.createdAt = Instant.now();
    }

    public UUID getEntryId() { return entryId; }
    public void setEntryId(UUID entryId) { this.entryId = entryId; }

    public UUID getWindowId() { return windowId; }
    public void setWindowId(UUID windowId) { this.windowId = windowId; }

    public String getPatientToken() { return patientToken; }
    public void setPatientToken(String patientToken) { this.patientToken = patientToken; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getCanonicalSha256() { return canonicalSha256; }
    public void setCanonicalSha256(String canonicalSha256) { this.canonicalSha256 = canonicalSha256; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getDataMinimizationNote() { return dataMinimizationNote; }
    public void setDataMinimizationNote(String dataMinimizationNote) { this.dataMinimizationNote = dataMinimizationNote; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
