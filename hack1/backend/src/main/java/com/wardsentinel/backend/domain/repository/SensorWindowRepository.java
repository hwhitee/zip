package com.wardsentinel.backend.domain.repository;

import com.wardsentinel.backend.domain.entity.SensorWindowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensorWindowRepository extends JpaRepository<SensorWindowEntity, UUID> {
    List<SensorWindowEntity> findByPatientIdOrderByStartEpochDesc(UUID patientId);
}
