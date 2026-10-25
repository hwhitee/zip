package com.wardsentinel.backend.controller;

import com.wardsentinel.backend.domain.entity.PrivacyLedgerEntity;
import com.wardsentinel.backend.domain.repository.PrivacyLedgerRepository;
import com.wardsentinel.backend.dto.PrivacyLedgerVerificationResponse;
import com.wardsentinel.backend.service.PrivacyLedgerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/privacy/ledger")
public class PrivacyLedgerController {

    private final PrivacyLedgerRepository privacyLedgerRepository;
    private final PrivacyLedgerService privacyLedgerService;

    public PrivacyLedgerController(
            PrivacyLedgerRepository privacyLedgerRepository,
            PrivacyLedgerService privacyLedgerService
    ) {
        this.privacyLedgerRepository = privacyLedgerRepository;
        this.privacyLedgerService = privacyLedgerService;
    }

    @GetMapping
    public List<PrivacyLedgerEntity> listEntries() {
        return privacyLedgerRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/verify")
    public PrivacyLedgerVerificationResponse verifyIntegrity() {
        return privacyLedgerService.verifyLedgerIntegrity();
    }
}
