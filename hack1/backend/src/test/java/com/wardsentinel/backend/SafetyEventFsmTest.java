package com.wardsentinel.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardsentinel.backend.domain.entity.SafetyEventEntity;
import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import com.wardsentinel.backend.domain.repository.OutboxEventRepository;
import com.wardsentinel.backend.domain.repository.SafetyEventRepository;
import com.wardsentinel.backend.service.PrivacyLedgerService;
import com.wardsentinel.backend.service.SafetyEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyEventFsmTest {

    @Mock
    private SafetyEventRepository safetyEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PrivacyLedgerService privacyLedgerService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SafetyEventService safetyEventService;

    @Test
    @DisplayName("Valid FSM: DETECTED -> ACKNOWLEDGED transitions properly")
    void testAcknowledgeTransition() {
        UUID eventId = UUID.randomUUID();
        SafetyEventEntity event = new SafetyEventEntity(
                eventId, UUID.randomUUID(), "WARD-4B", UUID.randomUUID(),
                RiskLevel.HIGH, SafetyEventStatus.DETECTED, 120000
        );

        when(safetyEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any(SafetyEventEntity.class))).thenAnswer(i -> i.getArgument(0));

        SafetyEventEntity result = safetyEventService.acknowledgeEvent(eventId, "Nurse Sarah", "Responding now");

        assertThat(result.getStatus()).isEqualTo(SafetyEventStatus.ACKNOWLEDGED);
        assertThat(result.getAcknowledgedAt()).isNotNull();
        assertThat(result.getAssignedTo()).isEqualTo("Nurse Sarah");
    }

    @Test
    @DisplayName("Invalid FSM: DETECTED -> RESOLVED directly is rejected with 409 CONFLICT")
    void testInvalidTransitionThrowsConflict() {
        UUID eventId = UUID.randomUUID();
        SafetyEventEntity event = new SafetyEventEntity(
                eventId, UUID.randomUUID(), "WARD-4B", UUID.randomUUID(),
                RiskLevel.HIGH, SafetyEventStatus.DETECTED, 120000
        );

        when(safetyEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> safetyEventService.resolveEvent(eventId, "Dr. Smith", "Bed exit checked"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid state transition");
    }
}
