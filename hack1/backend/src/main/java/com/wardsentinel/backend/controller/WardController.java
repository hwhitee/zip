package com.wardsentinel.backend.controller;

import com.wardsentinel.backend.domain.entity.BedEntity;
import com.wardsentinel.backend.domain.entity.InferenceResultEntity;
import com.wardsentinel.backend.domain.entity.PatientEntity;
import com.wardsentinel.backend.domain.entity.SafetyEventEntity;
import com.wardsentinel.backend.domain.model.BedStatus;
import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import com.wardsentinel.backend.domain.repository.BedRepository;
import com.wardsentinel.backend.domain.repository.InferenceResultRepository;
import com.wardsentinel.backend.domain.repository.PatientRepository;
import com.wardsentinel.backend.domain.repository.SafetyEventRepository;
import com.wardsentinel.backend.domain.repository.StaffRepository;
import com.wardsentinel.backend.dto.BedOverviewDto;
import com.wardsentinel.backend.dto.WardOverviewDto;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WardController {

    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;
    private final InferenceResultRepository inferenceResultRepository;
    private final SafetyEventRepository safetyEventRepository;
    private final StaffRepository staffRepository;

    public WardController(
            BedRepository bedRepository,
            PatientRepository patientRepository,
            InferenceResultRepository inferenceResultRepository,
            SafetyEventRepository safetyEventRepository,
            StaffRepository staffRepository
    ) {
        this.bedRepository = bedRepository;
        this.patientRepository = patientRepository;
        this.inferenceResultRepository = inferenceResultRepository;
        this.safetyEventRepository = safetyEventRepository;
        this.staffRepository = staffRepository;
    }

    @GetMapping("/wards/{wardId}/overview")
    public WardOverviewDto getWardOverview(@PathVariable String wardId) {
        List<BedEntity> beds = bedRepository.findByWardIdOrderByBedLabelAsc(wardId);
        List<BedOverviewDto> bedDtos = new ArrayList<>();

        int occupiedCount = 0;
        int activeEventsCount = 0;
        int criticalEventsCount = 0;

        for (BedEntity bed : beds) {
            BedOverviewDto dto = new BedOverviewDto();
            dto.setBedId(bed.getBedId());
            dto.setWardId(bed.getWardId());
            dto.setBedLabel(bed.getBedLabel());
            dto.setStatus(bed.getStatus());
            dto.setCurrentPatientId(bed.getCurrentPatientId());
            dto.setXCoord(bed.getXCoord());
            dto.setYCoord(bed.getYCoord());

            if (bed.getStatus() == BedStatus.OCCUPIED) {
                occupiedCount++;
            }

            if (bed.getCurrentPatientId() != null) {
                Optional<PatientEntity> patientOpt = patientRepository.findById(bed.getCurrentPatientId());
                patientOpt.ifPresent(p -> dto.setPatientDisplayName(p.getDisplayName()));

                Optional<InferenceResultEntity> latestResult = inferenceResultRepository.findFirstByPatientIdOrderByComputedAtDesc(bed.getCurrentPatientId());
                if (latestResult.isPresent()) {
                    dto.setLatestRiskScore(latestResult.get().getRiskScore());
                    dto.setLatestRiskLevel(latestResult.get().getRiskLevel());
                }

                List<SafetyEventEntity> activeEvents = safetyEventRepository.findByPatientIdAndStatusIn(
                        bed.getCurrentPatientId(),
                        List.of(SafetyEventStatus.DETECTED, SafetyEventStatus.ACKNOWLEDGED, SafetyEventStatus.INVESTIGATING, SafetyEventStatus.ESCALATED)
                );
                if (!activeEvents.isEmpty()) {
                    SafetyEventEntity active = activeEvents.getFirst();
                    dto.setActiveEventId(active.getEventId());
                    dto.setActiveEventSeverity(active.getSeverity());
                    dto.setAssignedNurseName(active.getAssignedTo());

                    activeEventsCount++;
                    if (active.getSeverity() == RiskLevel.CRITICAL) {
                        criticalEventsCount++;
                    }
                }
            }

            bedDtos.add(dto);
        }

        WardOverviewDto wardDto = new WardOverviewDto();
        wardDto.setWardId(wardId);
        wardDto.setTotalBeds(beds.size());
        wardDto.setOccupiedBeds(occupiedCount);
        wardDto.setActiveEventsCount(activeEventsCount);
        wardDto.setCriticalEventsCount(criticalEventsCount);
        wardDto.setOnShiftStaffCount(staffRepository.findByPrimaryWardIdAndOnShiftTrue(wardId).size());
        wardDto.setBeds(bedDtos);

        return wardDto;
    }

    @GetMapping("/beds")
    public List<BedEntity> listBeds(@RequestParam(required = false) String wardId) {
        if (wardId != null && !wardId.isBlank()) {
            return bedRepository.findByWardIdOrderByBedLabelAsc(wardId);
        }
        return bedRepository.findAll();
    }
}
