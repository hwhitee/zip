package com.wardsentinel.backend.domain.repository;

import com.wardsentinel.backend.domain.entity.StaffEntity;
import com.wardsentinel.backend.domain.model.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<StaffEntity, UUID> {
    List<StaffEntity> findByOnShiftTrueAndAvailableTrue();
    List<StaffEntity> findByPrimaryWardIdAndOnShiftTrue(String primaryWardId);
    List<StaffEntity> findByRoleAndOnShiftTrue(StaffRole role);
}
