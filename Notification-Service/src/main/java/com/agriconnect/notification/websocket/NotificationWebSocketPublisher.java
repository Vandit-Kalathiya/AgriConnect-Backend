package com.agriconnect.notification.websocket;

import com.agriconnect.notification.dto.NotificationResponse;
import com.agriconnect.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes a persisted notification to the user's WebSocket topic immediately
 * after it is saved by InAppDispatcher.
 *
 * UI subscribes to: /topic/notifications/{userId}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_PREFIX = "/topic/notifications/";

    public void pushToUser(Notification notification) {
        String destination = TOPIC_PREFIX + notification.getUserId();
        NotificationResponse payload = NotificationResponse.from(notification);
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("[WS] Pushed notification eventId={} to {}",
                      notification.getEventId(), destination);
        } catch (Exception ex) {
            // Non-critical — notification is already persisted in DB; client will
            // fetch it on next poll/reconnect.
            log.warn("[WS] Failed to push eventId={} to {}: {}",
                     notification.getEventId(), destination, ex.getMessage());
        }
    }
}
