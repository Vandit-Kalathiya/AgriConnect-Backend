package com.agriconnect.notification.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resolves a templateId to a subject + body string by substituting payload variables.
 * In production this would delegate to a database-backed or Thymeleaf template engine.
 * The current implementation provides sensible defaults for all core AgriConnect events.
 */
@Service
@Slf4j
public class TemplateResolver {

    private static final Map<String, String[]> TEMPLATES = Map.ofEntries(
        Map.entry("user.registered",     new String[]{"Welcome to AgriConnect!", "Hello {{userName}}, your account has been created successfully."}),
        Map.entry("user.password_reset", new String[]{"Password Reset OTP", "Your OTP for password reset is {{otp}}. Valid for 5 minutes."}),
        Map.entry("order.placed",        new String[]{"Order Placed", "Your order #{{orderId}} for {{cropName}} has been placed. Amount: ₹{{amount}}."}),
        Map.entry("order.confirmed",     new String[]{"Order Confirmed", "Great news! Your order #{{orderId}} has been confirmed by the farmer."}),
        Map.entry("order.cancelled",     new String[]{"Order Cancelled", "Your order #{{orderId}} has been cancelled. Reason: {{reason}}."}),
        Map.entry("payment.success",     new String[]{"Payment Successful", "Payment of ₹{{amount}} for order #{{orderId}} was successful."}),
        Map.entry("payment.failed",      new String[]{"Payment Failed", "Payment of ₹{{amount}} for order #{{orderId}} failed. Please retry."}),
        Map.entry("agreement.signed",    new String[]{"Agreement Signed", "Your contract agreement #{{agreementId}} has been signed by both parties."}),
        Map.entry("agreement.created",   new String[]{"New Agreement", "A new contract agreement #{{agreementId}} has been created for {{cropName}}."}),
        Map.entry("storage.booked",      new String[]{"Cold Storage Booked", "Cold storage at {{storageName}} has been booked from {{startDate}} to {{endDate}}."}),
        Map.entry("listing.approved",    new String[]{"Listing Approved", "Your listing for {{cropName}} has been approved and is now visible to buyers."})
    );

    public String resolveSubject(String templateId, Map<String, String> payload) {
        String[] template = TEMPLATES.getOrDefault(templateId, new String[]{"AgriConnect Notification", "You have a new notification."});
        return substitute(template[0], payload);
    }

    public String resolveBody(String templateId, Map<String, String> payload) {
        String[] template = TEMPLATES.getOrDefault(templateId, new String[]{"AgriConnect Notification", "You have a new notification."});
        return substitute(template[1], payload);
    }

    private String substitute(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
