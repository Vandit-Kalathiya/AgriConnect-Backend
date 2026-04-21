package com.agriconnect.Market.Access.App.kafka;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.avro.Priority;

import java.util.List;
import java.util.Map;

/**
 * Abstraction over Kafka notification publishing.
 * When feature.kafka.enabled=true → KafkaNotificationEventPublisher (real
 * sends).
 * When feature.kafka.enabled=false → NoOpNotificationEventPublisher (silent
 * no-op).
 */
public interface NotificationEventPublisher {

    void publish(String topic, NotificationEvent event);

    NotificationEvent buildEvent(String eventType,
            String userId,
            String templateId,
            List<String> channels,
            Map<String, String> payload,
            Priority priority,
            String correlationId,
            String recipientEmail,
            String recipientPhone);
}
