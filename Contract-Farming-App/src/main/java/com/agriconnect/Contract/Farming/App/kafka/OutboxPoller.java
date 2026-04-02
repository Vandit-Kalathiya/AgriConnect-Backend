package com.agriconnect.Contract.Farming.App.kafka;

import com.agriconnect.notification.avro.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int MAX_RETRIES = 5;

    private final NotificationOutboxRepository outboxRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void poll() {
        List<NotificationOutboxEntry> pending =
                outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(NotificationOutboxEntry.OutboxStatus.PENDING);

        if (pending.isEmpty()) return;

        for (NotificationOutboxEntry entry : pending) {
            try {
                NotificationEvent event = objectMapper.readValue(entry.getPayload(), NotificationEvent.class);
                kafkaTemplate.executeInTransaction(ops ->
                        ops.send(entry.getTopic(), entry.getPartitionKey(), event)
                );
                entry.setStatus(NotificationOutboxEntry.OutboxStatus.SENT);
                entry.setLastAttemptAt(Instant.now());
                log.info("[OUTBOX] Published eventId={}", entry.getEventId());
            } catch (Exception ex) {
                entry.setRetryCount(entry.getRetryCount() + 1);
                entry.setLastAttemptAt(Instant.now());
                if (entry.getRetryCount() >= MAX_RETRIES) {
                    entry.setStatus(NotificationOutboxEntry.OutboxStatus.FAILED);
                    log.error("[OUTBOX] Giving up on eventId={}", entry.getEventId(), ex);
                } else {
                    log.warn("[OUTBOX] Retry {}/{} for eventId={}", entry.getRetryCount(), MAX_RETRIES, entry.getEventId());
                }
            }
            outboxRepository.save(entry);
        }
    }
}
