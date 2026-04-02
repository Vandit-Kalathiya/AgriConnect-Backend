package com.agriconnect.notification.dispatcher;

import com.agriconnect.notification.exception.RetryableDispatchException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushDispatcher implements NotificationDispatcher {

    // optional=true so the service starts cleanly when firebase-service-account.json
    // is absent (local dev without FCM). The dispatch() method already guards on null.
    @Autowired(required = false)
    private FirebaseApp firebaseApp;

    private final RetryTemplate notificationRetryTemplate;
    private final MeterRegistry meterRegistry;

    public PushDispatcher(RetryTemplate notificationRetryTemplate, MeterRegistry meterRegistry) {
        this.notificationRetryTemplate = notificationRetryTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String channel() {
        return "PUSH";
    }

    @Override
    public void dispatch(DispatchContext ctx) {
        if (firebaseApp == null) {
            log.warn("[PUSH] Firebase not initialised — skipping push for eventId={}", ctx.getEventId());
            return;
        }

        String token = ctx.getFcmToken();
        if ("null".equals(token) || token == null || token.isBlank()) {
            log.warn("[PUSH] No FCM token for eventId={} userId={} — skipping", ctx.getEventId(), ctx.getUserId());
            return;
        }

        notificationRetryTemplate.execute(retryCtx -> {
            try {
                var fcmMessage = com.google.firebase.messaging.Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(ctx.getSubject())
                                .setBody(ctx.getBody())
                                .build())
                        .putData("eventId",   ctx.getEventId())
                        .putData("eventType", ctx.getEvent().getEventType())
                        .build();

                String response = FirebaseMessaging.getInstance(firebaseApp).send(fcmMessage);
                log.info("[PUSH] Sent eventId={} messageId={}", ctx.getEventId(), response);
                meterRegistry.counter("notification.dispatch.total", "channel", "PUSH", "status", "success").increment();
            } catch (FirebaseMessagingException ex) {
                log.error("[PUSH] FCM error for eventId={} attempt={}: {}", ctx.getEventId(), retryCtx.getRetryCount() + 1, ex.getMessage());
                meterRegistry.counter("notification.dispatch.total", "channel", "PUSH", "status", "failure").increment();
                if (ex.getMessagingErrorCode() == MessagingErrorCode.INTERNAL ||
                    ex.getMessagingErrorCode() == MessagingErrorCode.QUOTA_EXCEEDED) {
                    throw new RetryableDispatchException("FCM transient error", ex);
                }
                throw new RuntimeException("FCM non-retryable error", ex);
            }
            return null;
        });
    }
}
