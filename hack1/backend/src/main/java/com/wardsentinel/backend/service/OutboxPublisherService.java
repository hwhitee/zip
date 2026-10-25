package com.wardsentinel.backend.service;

import com.wardsentinel.backend.domain.entity.OutboxEventEntity;
import com.wardsentinel.backend.domain.model.OutboxStatus;
import com.wardsentinel.backend.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository, SimpMessagingTemplate messagingTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : pendingEvents) {
            try {
                messagingTemplate.convertAndSend(event.getTopic(), event.getPayload());
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
                log.debug("Published outbox event {} to topic {}", event.getId(), event.getTopic());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {} to topic {}: {}", event.getId(), event.getTopic(), ex.getMessage());
                event.setStatus(OutboxStatus.FAILED);
                outboxEventRepository.save(event);
            }
        }
    }
}
