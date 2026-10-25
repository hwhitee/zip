package com.wardsentinel.backend.domain.repository;

import com.wardsentinel.backend.domain.entity.InferenceResultEntity;
import com.wardsentinel.backend.domain.model.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InferenceResultRepository extends JpaRepository<InferenceResultEntity, UUID> {
    Optional<InferenceResultEntity> findByWindowId(UUID windowId);
    List<InferenceResultEntity> findByPatientIdOrderByComputedAtDesc(UUID patientId);
    Optional<InferenceResultEntity> findFirstByPatientIdOrderByComputedAtDesc(UUID patientId);
    List<InferenceResultEntity> findByRiskLevelInOrderByComputedAtDesc(List<RiskLevel> riskLevels);
}
