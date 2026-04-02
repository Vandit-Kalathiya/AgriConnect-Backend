package com.agriconnect.notification.dto;

import com.agriconnect.notification.entity.Notification;
import com.agriconnect.notification.entity.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable DTO sent over the WebSocket and returned from REST endpoints.
 * Decouples the wire format from the JPA entity.
 */
public record NotificationResponse(
        UUID   id,
        String eventId,
        String userId,
        String eventType,
        String sourceService,
        String templateId,
        String channel,
        NotificationStatus status,
        boolean read,
        String errorMessage,
        int    retryCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getEventId(),
                n.getUserId(),
                n.getEventType(),
                n.getSourceService(),
                n.getTemplateId(),
                n.getChannel(),
                n.getStatus(),
                n.isRead(),
                n.getErrorMessage(),
                n.getRetryCount(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
