package com.agriconnect.Generate.Agreement.App.kafka;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.avro.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
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

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Override
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
                .setSourceService("generate-agreement")
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
