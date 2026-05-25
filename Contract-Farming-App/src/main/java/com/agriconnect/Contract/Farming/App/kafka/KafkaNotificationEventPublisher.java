package com.agriconnect.Contract.Farming.App.kafka;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.avro.Priority;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "feature.kafka.enabled", havingValue = "true")
public class KafkaNotificationEventPublisher implements NotificationEventPublisher {

    private final NotificationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, NotificationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            NotificationOutboxEntry entry = NotificationOutboxEntry.builder()
                    .eventId(event.getEventId())
                    .topic(topic)
                    .partitionKey(event.getUserId() != null ? event.getUserId() : UUID.randomUUID().toString())
                    .payload(payload)
                    .status(NotificationOutboxEntry.OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(entry);
            log.info("[OUTBOX] Saved outbox entry eventId={} for topic={}", event.getEventId(), topic);
        } catch (Exception e) {
            log.error("[OUTBOX] Failed to write eventId={} to outbox", event.getEventId(), e);
            throw new RuntimeException("Failed to write notification to outbox", e);
        }
    }

    @Override
    public NotificationEvent buildEvent(String eventType,
                                        String userId,
                                        String templateId,
                                        List<String> channels,
                                        Map<String, String> payload,
                                        Priority priority,
                                        String correlationId,
                                        String recipientEmail,
                                        String recipientPhone) {
        return NotificationEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(eventType)
                .setSourceService("contract-farming")
                .setUserId(userId)
                .setChannels(channels)
                .setTemplateId(templateId)
                .setPayload(payload)
                .setPriority(priority)
                .setCorrelationId(correlationId)
                .setRecipientEmail(recipientEmail)
                .setRecipientPhone(recipientPhone)
                .setRecipientFcmToken(null)
                .setSchemaVersion(1)
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }
}
