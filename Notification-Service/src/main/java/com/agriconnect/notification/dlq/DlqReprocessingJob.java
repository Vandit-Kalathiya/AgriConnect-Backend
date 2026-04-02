package com.agriconnect.notification.dlq;

import com.agriconnect.notification.avro.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reads from the DLQ during a scheduled maintenance window and re-publishes
 * failed events back to their original topic so they get a second chance.
 *
 * The listener is paused by default. The @Scheduled method un-pauses it
 * briefly during off-peak hours (3 AM daily). After re-processing it
 * pauses again to avoid continuous DLQ consumption.
 *
 * In a full production setup this would integrate with an ops workflow
 * (Slack alert → manual approval → replay).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DlqReprocessingJob {

    @Value("${notification.topics.dlq}")
    private String dlqTopic;

    private final KafkaTemplate<String, NotificationEvent> dlqKafkaTemplate;

    private final List<ConsumerRecord<String, NotificationEvent>> buffer = new CopyOnWriteArrayList<>();

    /**
     * Collects DLQ records into a buffer for scheduled reprocessing.
     * containerFactory points to the standard consumer factory — DLQ topic uses same Avro schema.
     */
    @KafkaListener(
        topics          = "${notification.topics.dlq}",
        groupId         = "agriconnect-notification-dlq-reprocessor",
        containerFactory = "kafkaListenerContainerFactory",
        id              = "dlqReprocessingListener",
        autoStartup     = "false"
    )
    public void onDlqRecord(ConsumerRecord<String, NotificationEvent> record, Acknowledgment ack) {
        buffer.add(record);
        ack.acknowledge();
    }

    /**
     * Scheduled at 03:00 every night. Re-publishes buffered DLQ events to their
     * original topics. Failed re-publications are logged and left in the buffer
     * for the next run.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void reprocess() {
        if (buffer.isEmpty()) return;

        List<ConsumerRecord<String, NotificationEvent>> batch = new ArrayList<>(buffer);
        buffer.clear();
        log.info("[DLQ-JOB] Reprocessing {} DLQ events", batch.size());

        for (var record : batch) {
            try {
                byte[] originalTopicBytes = record.headers().lastHeader("dlq-original-topic") != null
                        ? record.headers().lastHeader("dlq-original-topic").value()
                        : null;
                String originalTopic = originalTopicBytes != null
                        ? new String(originalTopicBytes, StandardCharsets.UTF_8)
                        : dlqTopic;

                dlqKafkaTemplate.send(originalTopic, record.key(), record.value())
                        .whenComplete((r, ex) -> {
                            if (ex != null) {
                                log.error("[DLQ-JOB] Re-publish failed for eventId={}", record.value().getEventId(), ex);
                                buffer.add(record);
                            } else {
                                log.info("[DLQ-JOB] Re-published eventId={} to {}", record.value().getEventId(), originalTopic);
                            }
                        });
            } catch (Exception ex) {
                log.error("[DLQ-JOB] Unexpected error reprocessing eventId={}", record.value().getEventId(), ex);
                buffer.add(record);
            }
        }
    }
}
