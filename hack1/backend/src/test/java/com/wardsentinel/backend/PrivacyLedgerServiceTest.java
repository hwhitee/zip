package com.wardsentinel.backend;

import com.wardsentinel.backend.domain.entity.PrivacyLedgerEntity;
import com.wardsentinel.backend.domain.repository.PrivacyLedgerRepository;
import com.wardsentinel.backend.service.PrivacyLedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivacyLedgerServiceTest {

    @Mock
    private PrivacyLedgerRepository privacyLedgerRepository;

    @InjectMocks
    private PrivacyLedgerService privacyLedgerService;

    @Test
    @DisplayName("Generates valid SHA-256 hash linked to previous hash")
    void testRecordLedgerEntryGeneratesLinkedHash() {
        UUID windowId = UUID.randomUUID();
        when(privacyLedgerRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(privacyLedgerRepository.save(any(PrivacyLedgerEntity.class))).thenAnswer(i -> i.getArgument(0));

        PrivacyLedgerEntity entry = privacyLedgerService.recordLedgerEntry(
                windowId, "pt-12345678", "INGESTION", "Zero raw audio/video collected"
        );

        assertThat(entry.getPrevHash()).isEqualTo(PrivacyLedgerService.GENESIS_HASH);
        assertThat(entry.getCanonicalSha256()).hasSize(64);
    }

    @Test
    @DisplayName("Detects tampering when a recorded entry hash does not match content")
    void testDetectsTamperedLedger() {
        UUID id1 = UUID.randomUUID();
        UUID w1 = UUID.randomUUID();
        String c1 = PrivacyLedgerService.buildCanonicalString(w1, "pt-1", "INGESTION", PrivacyLedgerService.GENESIS_HASH, "Original note");
        String h1 = PrivacyLedgerService.computeSha256(c1);

        PrivacyLedgerEntity entry1 = new PrivacyLedgerEntity(
                id1, w1, "pt-1", "INGESTION", h1, PrivacyLedgerService.GENESIS_HASH, "Original note"
        );

        // Tamper with note
        entry1.setDataMinimizationNote("Altered unauthorized content");

        when(privacyLedgerRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entry1));

        var response = privacyLedgerService.verifyLedgerIntegrity();

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getTamperedEntryId()).isEqualTo(id1);
        assertThat(response.getMessage()).contains("Content tamper detected");
    }
}
