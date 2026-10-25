package com.wardsentinel.backend.service;

import com.wardsentinel.backend.domain.entity.StaffEntity;
import com.wardsentinel.backend.domain.model.StaffRole;
import com.wardsentinel.backend.domain.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class NurseDispatchService {

    private final StaffRepository staffRepository;

    public NurseDispatchService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public record DispatchResult(
            StaffEntity assignedStaff,
            boolean isEscalatedToChargeNurse,
            String dispatchNote
    ) {}

    @Transactional
    public DispatchResult dispatchNurseForEvent(String wardId, Double bedX, Double bedY) {
        double targetX = (bedX != null) ? bedX : 0.0;
        double targetY = (bedY != null) ? bedY : 0.0;

        // 1. Fetch staff satisfying HARD constraints: on shift, available, ward eligible
        List<StaffEntity> candidates = staffRepository.findByOnShiftTrueAndAvailableTrue()
                .stream()
                .filter(s -> s.isEligibleForWard(wardId))
                .filter(s -> s.getRole() == StaffRole.STAFF_NURSE || s.getRole() == StaffRole.NURSE_PRACTITIONER)
                .toList();

        if (!candidates.isEmpty()) {
            // 2. Rank candidates: distance + (workload * 15.0) - (primaryWard ? 20.0 : 0)
            StaffEntity bestNurse = candidates.stream()
                    .min(Comparator.comparingDouble(s -> calculateScore(s, wardId, targetX, targetY)))
                    .orElse(candidates.getFirst());

            bestNurse.setActiveAssignmentsCount(bestNurse.getActiveAssignmentsCount() + 1);
            staffRepository.save(bestNurse);

            return new DispatchResult(
                    bestNurse,
                    false,
                    "Dispatched to primary nurse: " + bestNurse.getName() + " (Active loads: " + bestNurse.getActiveAssignmentsCount() + ")"
            );
        }

        // 3. Fallback: No eligible staff nurse available -> Charge nurse escalation
        Optional<StaffEntity> chargeNurse = staffRepository.findByRoleAndOnShiftTrue(StaffRole.CHARGE_NURSE)
                .stream()
                .filter(cn -> cn.isEligibleForWard(wardId))
                .findFirst();

        if (chargeNurse.isPresent()) {
            StaffEntity cn = chargeNurse.get();
            cn.setActiveAssignmentsCount(cn.getActiveAssignmentsCount() + 1);
            staffRepository.save(cn);
            return new DispatchResult(
                    cn,
                    true,
                    "ESCALATION: No eligible staff nurse available. Assigned to Charge Nurse: " + cn.getName()
            );
        }

        return new DispatchResult(
                null,
                true,
                "ESCALATION PENDING: No nurse or charge nurse available on shift for ward " + wardId
        );
    }

    private double calculateScore(StaffEntity staff, String wardId, double targetX, double targetY) {
        double dx = staff.getCurrentX() - targetX;
        double dy = staff.getCurrentY() - targetY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        double workloadPenalty = staff.getActiveAssignmentsCount() * 15.0;
        double primaryWardBonus = staff.getPrimaryWardId().equalsIgnoreCase(wardId) ? -20.0 : 0.0;

        return distance + workloadPenalty + primaryWardBonus;
    }
}
