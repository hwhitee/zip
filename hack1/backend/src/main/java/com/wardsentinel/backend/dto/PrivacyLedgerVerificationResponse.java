package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class PrivacyLedgerVerificationResponse {

    @JsonProperty("verified")
    private boolean verified;

    @JsonProperty("totalEntriesChecked")
    private int totalEntriesChecked;

    @JsonProperty("tamperedEntryId")
    private UUID tamperedEntryId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("securityLimitationsNotice")
    private String securityLimitationsNotice;

    @JsonProperty("verifiedAt")
    private Instant verifiedAt;

    public PrivacyLedgerVerificationResponse() {}

    public PrivacyLedgerVerificationResponse(boolean verified, int totalEntriesChecked, UUID tamperedEntryId, String message) {
        this.verified = verified;
        this.totalEntriesChecked = totalEntriesChecked;
        this.tamperedEntryId = tamperedEntryId;
        this.message = message;
        this.securityLimitationsNotice = "LIMITS OF HASH-BASED TAMPER DETECTION: The SHA-256 hash chain detects modifications "
                + "and row alterations within the existing database ledger. It does NOT protect against full-database rollback "
                + "or deletion without an external trusted time-stamping authority or immutable append-only storage witness.";
        this.verifiedAt = Instant.now();
    }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public int getTotalEntriesChecked() { return totalEntriesChecked; }
    public void setTotalEntriesChecked(int totalEntriesChecked) { this.totalEntriesChecked = totalEntriesChecked; }

    public UUID getTamperedEntryId() { return tamperedEntryId; }
    public void setTamperedEntryId(UUID tamperedEntryId) { this.tamperedEntryId = tamperedEntryId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSecurityLimitationsNotice() { return securityLimitationsNotice; }
    public void setSecurityLimitationsNotice(String securityLimitationsNotice) { this.securityLimitationsNotice = securityLimitationsNotice; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
