package com.wardsentinel.backend.controller;

import com.wardsentinel.backend.domain.entity.StaffEntity;
import com.wardsentinel.backend.domain.repository.StaffRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffRepository staffRepository;

    public StaffController(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @GetMapping
    public List<StaffEntity> listStaff(@RequestParam(required = false) String wardId) {
        if (wardId != null && !wardId.isBlank()) {
            return staffRepository.findByPrimaryWardIdAndOnShiftTrue(wardId);
        }
        return staffRepository.findAll();
    }

    @GetMapping("/availability")
    public List<StaffEntity> getStaffAvailability() {
        return staffRepository.findByOnShiftTrueAndAvailableTrue();
    }
}
