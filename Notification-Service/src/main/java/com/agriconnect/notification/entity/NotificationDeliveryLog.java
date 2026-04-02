package com.agriconnect.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency guard — records each (eventId, channel) delivery attempt.
 * Before dispatching, the consumer checks this table to skip already-delivered events,
 * protecting against at-least-once redelivery producing duplicate notifications.
 */
@Entity
@Table(name = "notification_delivery_log",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_delivery_log_event_channel",
           columnNames = {"event_id", "channel"}
       ),
       indexes = {
           @Index(name = "idx_delivery_log_event_id", columnList = "event_id"),
           @Index(name = "idx_delivery_log_created_at", columnList = "created_at")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
