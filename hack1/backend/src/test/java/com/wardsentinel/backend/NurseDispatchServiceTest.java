package com.wardsentinel.backend;

import com.wardsentinel.backend.domain.entity.StaffEntity;
import com.wardsentinel.backend.domain.model.StaffRole;
import com.wardsentinel.backend.domain.repository.StaffRepository;
import com.wardsentinel.backend.service.NurseDispatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NurseDispatchServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private NurseDispatchService dispatchService;

    @Test
    @DisplayName("Dispatches closest eligible nurse with lowest workload")
    void testDispatchesBestEligibleNurse() {
        StaffEntity nurseClose = new StaffEntity(
                UUID.randomUUID(), "Nurse Sarah", StaffRole.STAFF_NURSE,
                true, true, "WARD-4B", "WARD-4B", 2.0, 2.0
        );
        nurseClose.setActiveAssignmentsCount(0);

        StaffEntity nurseFar = new StaffEntity(
                UUID.randomUUID(), "Nurse John", StaffRole.STAFF_NURSE,
                true, true, "WARD-4B", "WARD-4B", 20.0, 20.0
        );
        nurseFar.setActiveAssignmentsCount(2);

        when(staffRepository.findByOnShiftTrueAndAvailableTrue()).thenReturn(List.of(nurseClose, nurseFar));
        when(staffRepository.save(any(StaffEntity.class))).thenAnswer(i -> i.getArgument(0));

        var result = dispatchService.dispatchNurseForEvent("WARD-4B", 0.0, 0.0);

        assertThat(result.assignedStaff()).isNotNull();
        assertThat(result.assignedStaff().getName()).isEqualTo("Nurse Sarah");
        assertThat(result.isEscalatedToChargeNurse()).isFalse();
        assertThat(result.assignedStaff().getActiveAssignmentsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Escalates to charge nurse when no staff nurses available")
    void testEscalatesToChargeNurseWhenNoneAvailable() {
        when(staffRepository.findByOnShiftTrueAndAvailableTrue()).thenReturn(List.of());

        StaffEntity chargeNurse = new StaffEntity(
                UUID.randomUUID(), "Charge Nurse Alice", StaffRole.CHARGE_NURSE,
                true, true, "WARD-4B", "WARD-4B", 5.0, 5.0
        );

        when(staffRepository.findByRoleAndOnShiftTrue(StaffRole.CHARGE_NURSE)).thenReturn(List.of(chargeNurse));
        when(staffRepository.save(any(StaffEntity.class))).thenAnswer(i -> i.getArgument(0));

        var result = dispatchService.dispatchNurseForEvent("WARD-4B", 0.0, 0.0);

        assertThat(result.assignedStaff()).isNotNull();
        assertThat(result.assignedStaff().getName()).isEqualTo("Charge Nurse Alice");
        assertThat(result.isEscalatedToChargeNurse()).isTrue();
    }
}
