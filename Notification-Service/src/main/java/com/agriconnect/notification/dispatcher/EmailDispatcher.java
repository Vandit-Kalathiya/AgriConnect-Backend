package com.agriconnect.notification.dispatcher;

import com.agriconnect.notification.exception.RetryableDispatchException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailDispatcher implements NotificationDispatcher {

    private final JavaMailSender mailSender;
    private final RetryTemplate  notificationRetryTemplate;
    private final MeterRegistry  meterRegistry;

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public void dispatch(DispatchContext ctx) {
        String to = ctx.getRecipientEmail();
        if ("null".equals(to) || to == null || to.isBlank()) {
            log.warn("[EMAIL] No recipient for eventId={} — skipping", ctx.getEventId());
            return;
        }

        notificationRetryTemplate.execute(retryCtx -> {
            try {
                var message = mailSender.createMimeMessage();
                var helper  = new MimeMessageHelper(message, false, "UTF-8");
                helper.setTo(to);
                helper.setSubject(ctx.getSubject());
                helper.setText(ctx.getBody(), false);
                helper.setFrom("noreply@agriconnect.in");
                mailSender.send(message);
                log.info("[EMAIL] Sent eventId={} to={}", ctx.getEventId(), to);
                meterRegistry.counter("notification.dispatch.total", "channel", "EMAIL", "status", "success").increment();
            } catch (MailException | MessagingException ex) {
                // Both MailException (Spring) and MessagingException (Jakarta Mail) are
                // transient SMTP-layer failures that should be retried.
                log.error("[EMAIL] Transient failure for eventId={} attempt={}",
                          ctx.getEventId(), retryCtx.getRetryCount() + 1, ex);
                meterRegistry.counter("notification.dispatch.total", "channel", "EMAIL", "status", "failure").increment();
                throw new RetryableDispatchException("Email delivery failed", ex);
            } catch (RuntimeException ex) {
                log.error("[EMAIL] Non-retryable failure for eventId={}", ctx.getEventId(), ex);
                meterRegistry.counter("notification.dispatch.total", "channel", "EMAIL", "status", "failure").increment();
                throw ex;
            }
            return null;
        });
    }
}
