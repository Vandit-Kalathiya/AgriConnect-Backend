package com.agriconnect.notification.dispatcher;

import com.agriconnect.notification.avro.NotificationEvent;
import com.agriconnect.notification.template.TemplateResolver;
import lombok.Getter;

import java.util.Map;

/**
 * Carries all resolved data a dispatcher needs to deliver a single notification.
 * Built once by the router from the raw NotificationEvent + TemplateResolver output.
 */
@Getter
public class DispatchContext {

    private final NotificationEvent event;
    private final String subject;
    private final String body;

    public DispatchContext(NotificationEvent event, TemplateResolver resolver) {
        this.event   = event;
        Map<String, String> payload = event.getPayload();
        this.subject = resolver.resolveSubject(event.getTemplateId(), payload);
        this.body    = resolver.resolveBody(event.getTemplateId(), payload);
    }

    public String getUserId()         { return event.getUserId(); }
    public String getEventId()        { return event.getEventId(); }
    public String getCorrelationId()  { return String.valueOf(event.getCorrelationId()); }
    public String getRecipientEmail() { return String.valueOf(event.getRecipientEmail()); }
    public String getRecipientPhone() { return String.valueOf(event.getRecipientPhone()); }
    public String getFcmToken()       { return String.valueOf(event.getRecipientFcmToken()); }
}
