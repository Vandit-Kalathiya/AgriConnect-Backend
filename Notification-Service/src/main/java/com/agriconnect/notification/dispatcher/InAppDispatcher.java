package com.agriconnect.notification.dispatcher;

import com.agriconnect.notification.entity.Notification;
import com.agriconnect.notification.entity.NotificationStatus;
import com.agriconnect.notification.repository.NotificationRepository;
import com.agriconnect.notification.websocket.NotificationWebSocketPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InAppDispatcher implements NotificationDispatcher {

    private final NotificationRepository         notificationRepository;
    private final MeterRegistry                  meterRegistry;
    private final NotificationWebSocketPublisher wsPublisher;

    @Override
    public String channel() {
        return "IN_APP";
    }

    @Override
    public void dispatch(DispatchContext ctx) {
        Notification notification = Notification.builder()
                .eventId(ctx.getEventId())
                .userId(ctx.getUserId())
                .eventType(ctx.getEvent().getEventType())
                .sourceService(ctx.getEvent().getSourceService())
                .templateId(ctx.getEvent().getTemplateId())
                .channel("IN_APP")
                .status(NotificationStatus.SENT)
                .build();

        // 1. Persist to DB — source of truth
        Notification saved = notificationRepository.save(notification);
        log.info("[IN_APP] Persisted eventId={} userId={}", ctx.getEventId(), ctx.getUserId());

        // 2. Push to connected WebSocket client immediately (best-effort)
        wsPublisher.pushToUser(saved);

        meterRegistry.counter("notification.dispatch.total",
                              "channel", "IN_APP", "status", "success").increment();
    }
}
