package com.agriconnect.notification.dispatcher;

import com.agriconnect.notification.exception.RetryableDispatchException;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SmsDispatcher implements NotificationDispatcher {

    @Value("${twilio.phone-number}")
    private String fromNumber;

    private final RetryTemplate notificationRetryTemplate;
    private final MeterRegistry meterRegistry;

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public void dispatch(DispatchContext ctx) {
        String to = ctx.getRecipientPhone();
        if ("null".equals(to) || to == null || to.isBlank()) {
            log.warn("[SMS] No recipient phone for eventId={} — skipping", ctx.getEventId());
            return;
        }

        notificationRetryTemplate.execute(retryCtx -> {
            try {
                Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber), ctx.getBody()).create();
                log.info("[SMS] Sent eventId={} to={}", ctx.getEventId(), to);
                meterRegistry.counter("notification.dispatch.total", "channel", "SMS", "status", "success").increment();
            } catch (ApiException ex) {
                log.error("[SMS] Twilio API error for eventId={} attempt={}: {}",
                          ctx.getEventId(), retryCtx.getRetryCount() + 1, ex.getMessage());
                meterRegistry.counter("notification.dispatch.total", "channel", "SMS", "status", "failure").increment();
                if (ex.getStatusCode() >= 500) {
                    throw new RetryableDispatchException("Twilio transient error", ex);
                }
                throw ex;
            }
            return null;
        });
    }
}
