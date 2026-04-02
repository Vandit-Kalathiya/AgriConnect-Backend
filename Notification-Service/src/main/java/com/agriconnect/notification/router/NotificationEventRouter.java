package com.agriconnect.notification.router;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.dispatcher.DispatchContext;
import com.agriconnect.notification.dispatcher.NotificationDispatcher;
import com.agriconnect.notification.entity.NotificationDeliveryLog;
import com.agriconnect.notification.entity.NotificationStatus;
import com.agriconnect.notification.repository.NotificationDeliveryLogRepository;
import com.agriconnect.notification.template.TemplateResolver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationEventRouter {

    private final Map<String, NotificationDispatcher>    dispatchers;
    private final NotificationDeliveryLogRepository      deliveryLogRepo;
    private final TemplateResolver                       templateResolver;
    private final MeterRegistry                          meterRegistry;

    public NotificationEventRouter(List<NotificationDispatcher> dispatcherList,
                                   NotificationDeliveryLogRepository deliveryLogRepo,
                                   TemplateResolver templateResolver,
                                   MeterRegistry meterRegistry) {
        this.dispatchers      = dispatcherList.stream()
                .collect(Collectors.toMap(NotificationDispatcher::channel, Function.identity()));
        this.deliveryLogRepo  = deliveryLogRepo;
        this.templateResolver = templateResolver;
        this.meterRegistry    = meterRegistry;
    }

    /**
     * Routes a NotificationEvent to each requested channel.
     * Idempotency is enforced per (eventId, channel): if the delivery log already
     * contains a SENT record the channel is skipped without re-dispatch.
     */
    @Transactional
    public void route(NotificationEvent event) {
        var context = new DispatchContext(event, templateResolver);

        for (Object ch : event.getChannels()) {
            String channel = ch.toString();
            dispatchChannel(context, channel);
        }
    }

    private void dispatchChannel(DispatchContext ctx, String channel) {
        String eventId = ctx.getEventId();

        if (deliveryLogRepo.existsByEventIdAndChannel(eventId, channel)) {
            log.info("[ROUTER] Skipping duplicate delivery eventId={} channel={}", eventId, channel);
            meterRegistry.counter("notification.dispatch.total", "channel", channel, "status", "duplicate").increment();
            return;
        }

        NotificationDispatcher dispatcher = dispatchers.get(channel);
        if (dispatcher == null) {
            log.warn("[ROUTER] No dispatcher registered for channel={} eventId={}", channel, eventId);
            return;
        }

        Timer.Sample timer = Timer.start(meterRegistry);
        try {
            dispatcher.dispatch(ctx);
            recordDelivery(eventId, channel, NotificationStatus.SENT);
        } catch (Exception ex) {
            recordDelivery(eventId, channel, NotificationStatus.FAILED);
            throw ex;
        } finally {
            timer.stop(Timer.builder("notification.dispatch.duration")
                    .tag("channel", channel)
                    .register(meterRegistry));
        }
    }

    private void recordDelivery(String eventId, String channel, NotificationStatus status) {
        deliveryLogRepo.save(NotificationDeliveryLog.builder()
                .eventId(eventId)
                .channel(channel)
                .status(status)
                .build());
    }
}
