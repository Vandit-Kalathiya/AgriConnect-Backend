package com.agriconnect.api.gateway.Service.Email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("AgriConnect — Password Reset OTP");
            helper.setText(buildOtpEmailBody(otp), true);

            mailSender.send(message);
            logger.info("Password reset OTP email sent to: {}", to);

        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    private String buildOtpEmailBody(String otp) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto; padding: 24px;
                            border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #2e7d32;">AgriConnect — Password Reset</h2>
                    <p>You requested a password reset. Use the OTP below to proceed:</p>
                    <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px;
                                color: #1b5e20; text-align: center; padding: 16px 0;">
                        %s
                    </div>
                    <p style="color: #666;">This OTP is valid for <strong>10 minutes</strong>.</p>
                    <p style="color: #666;">If you did not request this, please ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 16px 0;">
                    <p style="font-size: 12px; color: #999;">AgriConnect — Connecting Farmers &amp; Buyers</p>
                </div>
                """.formatted(otp);
    }
}
