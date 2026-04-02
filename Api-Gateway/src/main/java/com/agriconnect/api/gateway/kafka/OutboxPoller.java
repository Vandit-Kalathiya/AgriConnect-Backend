package com.agriconnect.api.gateway.kafka;

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

/**
 * Polls the notification_outbox table every 5 seconds and publishes PENDING entries
 * to their Kafka topics. This ensures at-least-once delivery even if Kafka was
 * unavailable when the original business transaction committed.
 *
 * The poll is transactional: entries are marked SENT only after a successful publish.
 * Entries that fail 5 times are marked FAILED and excluded from future polling.
 */
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
        log.debug("[OUTBOX] Processing {} pending outbox entries", pending.size());

        for (NotificationOutboxEntry entry : pending) {
            try {
                NotificationEvent event = objectMapper.readValue(entry.getPayload(), NotificationEvent.class);
                kafkaTemplate.executeInTransaction(ops ->
                        ops.send(entry.getTopic(), entry.getPartitionKey(), event)
                );
                entry.setStatus(NotificationOutboxEntry.OutboxStatus.SENT);
                entry.setLastAttemptAt(Instant.now());
                log.info("[OUTBOX] Published eventId={} to {}", entry.getEventId(), entry.getTopic());
            } catch (Exception ex) {
                entry.setRetryCount(entry.getRetryCount() + 1);
                entry.setLastAttemptAt(Instant.now());
                if (entry.getRetryCount() >= MAX_RETRIES) {
                    entry.setStatus(NotificationOutboxEntry.OutboxStatus.FAILED);
                    log.error("[OUTBOX] Giving up on eventId={} after {} retries", entry.getEventId(), MAX_RETRIES, ex);
                } else {
                    log.warn("[OUTBOX] Retry {}/{} for eventId={}", entry.getRetryCount(), MAX_RETRIES, entry.getEventId(), ex);
                }
            }
            outboxRepository.save(entry);
        }
    }
}
