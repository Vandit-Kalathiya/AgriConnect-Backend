package com.agriconnect.notification.dlq;

import com.agriconnect.notification.avro.NotificationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class DlqPublisher {

    @Value("${notification.topics.dlq}")
    private String dlqTopic;

    private final KafkaTemplate<String, NotificationEvent> dlqKafkaTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Forwards a failed record to the DLQ topic with diagnostic headers.
     * The original key (userId) is preserved so DLQ consumers can re-partition correctly.
     */
    public void send(ConsumerRecord<String, NotificationEvent> original, Throwable cause) {
        var headers = new RecordHeaders(original.headers());
        headers.add("dlq-original-topic",    original.topic().getBytes(StandardCharsets.UTF_8));
        headers.add("dlq-original-partition", String.valueOf(original.partition()).getBytes(StandardCharsets.UTF_8));
        headers.add("dlq-original-offset",    String.valueOf(original.offset()).getBytes(StandardCharsets.UTF_8));
        headers.add("dlq-exception-class",    cause.getClass().getName().getBytes(StandardCharsets.UTF_8));
        headers.add("dlq-exception-message",  (cause.getMessage() != null ? cause.getMessage() : "unknown")
                                               .getBytes(StandardCharsets.UTF_8));
        headers.add("dlq-timestamp",          String.valueOf(Instant.now().toEpochMilli()).getBytes(StandardCharsets.UTF_8));

        var record = new ProducerRecord<>(dlqTopic, null, original.key(), original.value(), headers);
        dlqKafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[DLQ] Failed to publish to DLQ for eventId={}", original.value().getEventId(), ex);
                    } else {
                        log.warn("[DLQ] Published eventId={} from topic={} to DLQ",
                                 original.value().getEventId(), original.topic());
                        meterRegistry.counter("notification.dlq.total").increment();
                    }
                });
    }
}
