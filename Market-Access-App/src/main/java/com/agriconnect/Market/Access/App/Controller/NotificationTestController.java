package com.agriconnect.Market.Access.App.Controller;

import com.agriconnect.Market.Access.App.kafka.NotificationEventPublisher;
import com.agriconnect.notification.avro.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications/test")
@RequiredArgsConstructor
public class NotificationTestController {

    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${notification.topics.market}")
    private String marketTopic;

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishTestNotification(
            @RequestParam("userId") String userId,
            @RequestParam(value = "eventType", defaultValue = "MARKET_TEST_EVENT") String eventType,
            @RequestParam(value = "templateId", defaultValue = "market.test") String templateId,
            @RequestParam(value = "recipientEmail", required = false) String recipientEmail,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        Map<String, String> eventPayload = new HashMap<>();
        if (payload != null && !payload.isEmpty()) {
            eventPayload.putAll(payload);
        } else {
            eventPayload.put("message", "Market Access test notification");
            eventPayload.put("source", "manual-test-endpoint");
        }

        var event = notificationEventPublisher.buildEvent(
                eventType,
                userId,
                templateId,
                List.of("EMAIL", "IN_APP"),
                eventPayload,
                Priority.NORMAL,
                "market-test-correlation",
                recipientEmail,
                null
        );

        notificationEventPublisher.publish(marketTopic, event);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "PUBLISHED");
        response.put("topic", marketTopic);
        response.put("eventId", event.getEventId().toString());
        response.put("userId", userId);
        return ResponseEntity.ok(response);
    }
}
