package com.agriconnect.notification.consumer;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.dlq.DlqPublisher;
import com.agriconnect.notification.exception.RetryableDispatchException;
import com.agriconnect.notification.router.NotificationEventRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Single-entry-point Kafka consumer for all AgriConnect notification topics.
 *
 * Offset management: MANUAL_IMMEDIATE — the offset is committed only after
 * successful routing. This prevents message loss on crash mid-processing.
 *
 * Error handling:
 *  - RetryableDispatchException → re-thrown so Spring-Retry retries the dispatcher.
 *  - Any other exception after retries exhausted → forwarded to DLQ; offset committed
 *    to prevent the poison-pill from blocking the partition.
 *
 * Graceful shutdown: Spring Boot's SmartLifecycle stops the container before
 * the application context closes, draining in-flight records.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationEventRouter router;
    private final DlqPublisher            dlqPublisher;

    @KafkaListener(
        topics = {
            "${notification.topics.auth}",
            "${notification.topics.market}",
            "${notification.topics.contract}",
            "${notification.topics.agreement}"
        },
        groupId          = "agriconnect-notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, NotificationEvent> record, Acknowledgment ack) {
        NotificationEvent event = record.value();

        MDC.put("eventId",       event.getEventId());
        MDC.put("correlationId", String.valueOf(event.getCorrelationId()));
        MDC.put("userId",        event.getUserId());
        MDC.put("topic",         record.topic());

        try {
            log.debug("[CONSUMER] Received eventId={} type={} topic={} partition={} offset={}",
                    event.getEventId(), event.getEventType(),
                    record.topic(), record.partition(), record.offset());

            router.route(event);
            ack.acknowledge();

            log.info("[CONSUMER] Processed eventId={} type={}", event.getEventId(), event.getEventType());

        } catch (RetryableDispatchException ex) {
            // Spring-Retry will retry — do NOT acknowledge here
            log.warn("[CONSUMER] Retryable failure eventId={}: {}", event.getEventId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[CONSUMER] Non-retryable failure for eventId={} — routing to DLQ", event.getEventId(), ex);
            dlqPublisher.send(record, ex);
            ack.acknowledge();  // must ack to prevent this record from blocking the partition forever
        } finally {
            MDC.clear();
        }
    }
}
