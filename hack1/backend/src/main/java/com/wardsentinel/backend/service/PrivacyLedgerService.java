package com.wardsentinel.backend.service;

import com.wardsentinel.backend.domain.entity.PrivacyLedgerEntity;
import com.wardsentinel.backend.domain.repository.PrivacyLedgerRepository;
import com.wardsentinel.backend.dto.PrivacyLedgerVerificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class PrivacyLedgerService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final PrivacyLedgerRepository privacyLedgerRepository;

    public PrivacyLedgerService(PrivacyLedgerRepository privacyLedgerRepository) {
        this.privacyLedgerRepository = privacyLedgerRepository;
    }

    @Transactional
    public PrivacyLedgerEntity recordLedgerEntry(UUID windowId, String patientToken, String eventType, String dataMinimizationNote) {
        String prevHash = privacyLedgerRepository.findFirstByOrderByCreatedAtDesc()
                .map(PrivacyLedgerEntity::getCanonicalSha256)
                .orElse(GENESIS_HASH);

        String canonicalData = buildCanonicalString(windowId, patientToken, eventType, prevHash, dataMinimizationNote);
        String currentHash = computeSha256(canonicalData);

        PrivacyLedgerEntity entry = new PrivacyLedgerEntity(
                UUID.randomUUID(),
                windowId,
                patientToken,
                eventType,
                currentHash,
                prevHash,
                dataMinimizationNote
        );

        return privacyLedgerRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PrivacyLedgerVerificationResponse verifyLedgerIntegrity() {
        List<PrivacyLedgerEntity> entries = privacyLedgerRepository.findAllByOrderByCreatedAtDesc();
        // Reverse to check in chronological order
        List<PrivacyLedgerEntity> chronological = entries.reversed();

        String expectedPrevHash = GENESIS_HASH;
        int checkedCount = 0;

        for (PrivacyLedgerEntity entry : chronological) {
            checkedCount++;

            // 1. Check prev_hash link
            if (!entry.getPrevHash().equalsIgnoreCase(expectedPrevHash)) {
                return new PrivacyLedgerVerificationResponse(
                        false,
                        checkedCount,
                        entry.getEntryId(),
                        "Hash chain broken at entry " + entry.getEntryId() + ". Expected prevHash: " + expectedPrevHash + ", found: " + entry.getPrevHash()
                );
            }

            // 2. Check canonical hash recalculation
            String canonicalData = buildCanonicalString(
                    entry.getWindowId(),
                    entry.getPatientToken(),
                    entry.getEventType(),
                    entry.getPrevHash(),
                    entry.getDataMinimizationNote()
            );
            String computedHash = computeSha256(canonicalData);

            if (!entry.getCanonicalSha256().equalsIgnoreCase(computedHash)) {
                return new PrivacyLedgerVerificationResponse(
                        false,
                        checkedCount,
                        entry.getEntryId(),
                        "Content tamper detected at entry " + entry.getEntryId() + ". Hash recalculation mismatch."
                );
            }

            expectedPrevHash = entry.getCanonicalSha256();
        }

        return new PrivacyLedgerVerificationResponse(
                true,
                checkedCount,
                null,
                "All " + checkedCount + " privacy ledger entries cryptographically verified against SHA-256 hash chain."
        );
    }

    public static String buildCanonicalString(UUID windowId, String patientToken, String eventType, String prevHash, String note) {
        return "window:" + (windowId != null ? windowId.toString() : "")
                + "|token:" + (patientToken != null ? patientToken : "")
                + "|event:" + (eventType != null ? eventType : "")
                + "|prev:" + (prevHash != null ? prevHash : "")
                + "|note:" + (note != null ? note : "");
    }

    public static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
