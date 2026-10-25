package com.wardsentinel.backend.domain.repository;

import com.wardsentinel.backend.domain.entity.SafetyEventEntity;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SafetyEventRepository extends JpaRepository<SafetyEventEntity, UUID> {
    List<SafetyEventEntity> findByWardIdOrderByDetectedAtDesc(String wardId);
    List<SafetyEventEntity> findByWardIdAndStatusInOrderByDetectedAtDesc(String wardId, List<SafetyEventStatus> statuses);
    List<SafetyEventEntity> findByPatientIdOrderByDetectedAtDesc(UUID patientId);
    Optional<SafetyEventEntity> findByTriggeringResultId(UUID triggeringResultId);
    List<SafetyEventEntity> findByPatientIdAndStatusIn(UUID patientId, List<SafetyEventStatus> statuses);
}
