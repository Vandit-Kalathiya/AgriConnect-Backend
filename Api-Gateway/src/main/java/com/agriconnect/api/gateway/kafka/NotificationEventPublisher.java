package com.agriconnect.api.gateway.kafka;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.avro.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes NotificationEvent records to Kafka using transactional sends.
 * A transactional send guarantees exactly-once delivery to the broker (no duplicates
 * on retry). The Outbox pattern in OutboxPoller provides additional durability if
 * Kafka is temporarily unavailable at publish time.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void publish(String topic, NotificationEvent event) {
        kafkaTemplate.executeInTransaction(ops ->
            ops.send(topic, event.getUserId(), event)
               .whenComplete((result, ex) -> {
                   if (ex != null) {
                       log.error("[KAFKA] Publish failed eventId={} topic={}", event.getEventId(), topic, ex);
                   } else {
                       log.info("[KAFKA] Published eventId={} partition={} offset={}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                   }
               })
        );
    }

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
                .setSourceService("api-gateway")
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
