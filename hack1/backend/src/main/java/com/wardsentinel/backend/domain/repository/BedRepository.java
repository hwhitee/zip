package com.wardsentinel.backend.domain.repository;

import com.wardsentinel.backend.domain.entity.BedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BedRepository extends JpaRepository<BedEntity, UUID> {
    List<BedEntity> findByWardIdOrderByBedLabelAsc(String wardId);
    Optional<BedEntity> findByWardIdAndBedLabel(String wardId, String bedLabel);
}
