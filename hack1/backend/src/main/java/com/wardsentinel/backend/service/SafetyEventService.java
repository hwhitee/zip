package com.wardsentinel.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardsentinel.backend.domain.entity.OutboxEventEntity;
import com.wardsentinel.backend.domain.entity.SafetyEventEntity;
import com.wardsentinel.backend.domain.model.SafetyEventStatus;
import com.wardsentinel.backend.domain.repository.OutboxEventRepository;
import com.wardsentinel.backend.domain.repository.SafetyEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SafetyEventService {

    private final SafetyEventRepository safetyEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final PrivacyLedgerService privacyLedgerService;

    public SafetyEventService(
            SafetyEventRepository safetyEventRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            PrivacyLedgerService privacyLedgerService
    ) {
        this.safetyEventRepository = safetyEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.privacyLedgerService = privacyLedgerService;
    }

    @Transactional
    public SafetyEventEntity acknowledgeEvent(UUID eventId, String assignedTo, String note) {
        SafetyEventEntity event = getEventOrThrow(eventId);
        assertTransition(event, SafetyEventStatus.ACKNOWLEDGED);

        event.setStatus(SafetyEventStatus.ACKNOWLEDGED);
        event.setAcknowledgedAt(Instant.now());
        if (assignedTo != null && !assignedTo.isBlank()) {
            event.setAssignedTo(assignedTo);
        }
        appendNote(event, assignedTo != null ? assignedTo : "System", "ACKNOWLEDGED: " + (note != null ? note : "Acknowledged by nurse"));
        event.setUpdatedAt(Instant.now());

        SafetyEventEntity saved = safetyEventRepository.save(event);
        publishEventUpdate(saved, "SAFETY_EVENT_ACKNOWLEDGED");
        recordPrivacyLedger(saved, "EVENT_ACKNOWLEDGED", "Nurse acknowledged event with zero raw payload exposure");
        return saved;
    }

    @Transactional
    public SafetyEventEntity investigateEvent(UUID eventId, String author, String note) {
        SafetyEventEntity event = getEventOrThrow(eventId);
        assertTransition(event, SafetyEventStatus.INVESTIGATING);

        event.setStatus(SafetyEventStatus.INVESTIGATING);
        appendNote(event, author != null ? author : "Staff", "INVESTIGATING: " + note);
        event.setUpdatedAt(Instant.now());

        SafetyEventEntity saved = safetyEventRepository.save(event);
        publishEventUpdate(saved, "SAFETY_EVENT_INVESTIGATING");
        return saved;
    }

    @Transactional
    public SafetyEventEntity escalateEvent(UUID eventId, String author, String note, String escalatedTo) {
        SafetyEventEntity event = getEventOrThrow(eventId);
        assertTransition(event, SafetyEventStatus.ESCALATED);

        event.setStatus(SafetyEventStatus.ESCALATED);
        if (escalatedTo != null && !escalatedTo.isBlank()) {
            event.setAssignedTo(escalatedTo);
        }
        appendNote(event, author != null ? author : "System", "ESCALATED to " + escalatedTo + ": " + note);
        event.setUpdatedAt(Instant.now());

        SafetyEventEntity saved = safetyEventRepository.save(event);
        publishEventUpdate(saved, "SAFETY_EVENT_ESCALATED");
        return saved;
    }

    @Transactional
    public SafetyEventEntity resolveEvent(UUID eventId, String author, String note) {
        SafetyEventEntity event = getEventOrThrow(eventId);
        assertTransition(event, SafetyEventStatus.RESOLVED);

        event.setStatus(SafetyEventStatus.RESOLVED);
        event.setResolvedAt(Instant.now());
        appendNote(event, author != null ? author : "Staff", "RESOLVED: " + note);
        event.setUpdatedAt(Instant.now());

        SafetyEventEntity saved = safetyEventRepository.save(event);
        publishEventUpdate(saved, "SAFETY_EVENT_RESOLVED");
        recordPrivacyLedger(saved, "EVENT_RESOLVED", "Event closed following nurse bedside intervention");
        return saved;
    }

    @Transactional
    public SafetyEventEntity dismissEvent(UUID eventId, String author, String reason) {
        SafetyEventEntity event = getEventOrThrow(eventId);
        assertTransition(event, SafetyEventStatus.DISMISSED);

        event.setStatus(SafetyEventStatus.DISMISSED);
        event.setResolvedAt(Instant.now());
        appendNote(event, author != null ? author : "Clinician", "DISMISSED (False Positive Override): " + reason);
        event.setUpdatedAt(Instant.now());

        SafetyEventEntity saved = safetyEventRepository.save(event);
        publishEventUpdate(saved, "SAFETY_EVENT_DISMISSED");
        return saved;
    }

    @Transactional
    public SafetyEventEntity assignNurse(UUID eventId, String assignedTo, String assignedBy) {
        SafetyEventEntity event = getEventOrThrow(eventId);
        event.setAssignedTo(assignedTo);
        appendNote(event, assignedBy != null ? assignedBy : "System", "Assigned to " + assignedTo);
        event.setUpdatedAt(Instant.now());

        SafetyEventEntity saved = safetyEventRepository.save(event);
        publishEventUpdate(saved, "SAFETY_EVENT_ASSIGNED");
        return saved;
    }

    private SafetyEventEntity getEventOrThrow(UUID eventId) {
        return safetyEventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SafetyEvent " + eventId + " not found"));
    }

    private void assertTransition(SafetyEventEntity event, SafetyEventStatus target) {
        if (!event.getStatus().canTransitionTo(target)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid state transition from " + event.getStatus() + " to " + target
            );
        }
    }

    private void appendNote(SafetyEventEntity event, String author, String text) {
        try {
            List<Map<String, String>> notesList = new ArrayList<>();
            if (event.getNotes() != null && !event.getNotes().isBlank() && !event.getNotes().equals("[]")) {
                List<?> parsed = objectMapper.readValue(event.getNotes(), List.class);
                for (Object item : parsed) {
                    if (item instanceof Map) {
                        notesList.add((Map<String, String>) item);
                    }
                }
            }
            notesList.add(Map.of(
                    "ts", Instant.now().toString(),
                    "author", author,
                    "text", text
            ));
            event.setNotes(objectMapper.writeValueAsString(notesList));
        } catch (Exception e) {
            event.setNotes("[{\"ts\":\"" + Instant.now() + "\",\"author\":\"" + author + "\",\"text\":\"" + text + "\"}]");
        }
    }

    private void publishEventUpdate(SafetyEventEntity event, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventEntity outbox = new OutboxEventEntity(
                    UUID.randomUUID(),
                    "SafetyEvent",
                    event.getEventId().toString(),
                    eventType,
                    "/topic/ward/" + event.getWardId() + "/alerts",
                    payload
            );
            outboxEventRepository.save(outbox);
        } catch (Exception ex) {
            // Log without failing transaction
        }
    }

    private void recordPrivacyLedger(SafetyEventEntity event, String eventType, String note) {
        try {
            privacyLedgerService.recordLedgerEntry(
                    event.getEventId(),
                    "pt-" + event.getPatientId().toString().substring(0, 8),
                    eventType,
                    note
            );
        } catch (Exception ignored) {}
    }
}
