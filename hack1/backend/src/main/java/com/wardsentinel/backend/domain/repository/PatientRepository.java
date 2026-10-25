package com.wardsentinel.backend.domain.repository;

import com.wardsentinel.backend.domain.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {
    List<PatientEntity> findByWardIdAndStatus(String wardId, String status);
}
