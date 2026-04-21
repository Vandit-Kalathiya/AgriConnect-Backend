package com.agriconnect.Generate.Agreement.App.kafka;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.avro.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@ConditionalOnProperty(name = "feature.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpNotificationEventPublisher implements NotificationEventPublisher {

    public NoOpNotificationEventPublisher() {
        log.warn("[KAFKA] Kafka is DISABLED (feature.kafka.enabled=false). " +
                 "All notification publishes will be silent no-ops.");
    }

    @Override
    public void publish(String topic, NotificationEvent event) {
        log.debug("[KAFKA-NOOP] Skipping publish to topic={} — Kafka feature is disabled.", topic);
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
        return null;
    }
}
