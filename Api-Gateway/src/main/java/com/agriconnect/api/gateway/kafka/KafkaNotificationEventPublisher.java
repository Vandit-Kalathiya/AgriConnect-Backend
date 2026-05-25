package com.agriconnect.api.gateway.kafka;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.avro.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
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

    /**
     * Serialize a NotificationEvent to JSON using Avro's own JsonEncoder.
     * <p>
     * We intentionally do NOT use Jackson's ObjectMapper here.
     * Avro-generated classes carry a static {@code SCHEMA$} field of type
     * {@code org.apache.avro.Schema}. When Jackson introspects the bean it
     * tries to serialize that Schema object too, which causes:
     * <pre>AvroRuntimeException: Not an enum: {type:record, name:NotificationEvent…}</pre>
     * because Jackson calls {@code Schema.RecordSchema#getEnumSymbols()} on a
     * record schema rather than an enum schema.
     * Avro's own encoder understands its type system and handles Priority enums,
     * nullable unions, and logical types correctly.
     */
    private static String toAvroJson(NotificationEvent event) {
        try {
            DatumWriter<NotificationEvent> writer = new SpecificDatumWriter<>(NotificationEvent.SCHEMA$);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(NotificationEvent.SCHEMA$, out);
            writer.write(event, encoder);
            encoder.flush();
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize NotificationEvent to Avro JSON", e);
        }
    }

    @Override
    public void publish(String topic, NotificationEvent event) {
        try {
            // Use Avro JSON serialization — Jackson cannot handle Avro Schema objects
            String payload = toAvroJson(event);
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
