package com.wardsentinel.backend.domain.repository;

import com.wardsentinel.backend.domain.entity.PrivacyLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrivacyLedgerRepository extends JpaRepository<PrivacyLedgerEntity, UUID> {
    List<PrivacyLedgerEntity> findByWindowIdOrderByCreatedAtAsc(UUID windowId);
    List<PrivacyLedgerEntity> findAllByOrderByCreatedAtDesc();
    Optional<PrivacyLedgerEntity> findFirstByOrderByCreatedAtDesc();
}
