package com.agriconnect.api.gateway.kafka;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional Outbox entry. The publisher writes this row in the same DB transaction
 * as the business operation. The OutboxPoller reads PENDING rows and publishes them to Kafka,
 * guaranteeing at-least-once delivery even if Kafka is temporarily unavailable.
 */
@Entity
@Table(name = "notification_outbox",
       indexes = {
           @Index(name = "idx_outbox_status",     columnList = "status"),
           @Index(name = "idx_outbox_created_at", columnList = "created_at")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationOutboxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "partition_key", nullable = false, length = 64)
    private String partitionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    public enum OutboxStatus { PENDING, SENT, FAILED }
}
