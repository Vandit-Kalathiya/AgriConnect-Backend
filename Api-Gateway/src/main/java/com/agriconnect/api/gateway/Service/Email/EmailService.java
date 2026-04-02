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
            helper.setText(buildPasswordResetEmailBody(otp), true);

            mailSender.send(message);
            logger.info("Password reset OTP email sent to: {}", to);

        } catch (MessagingException e) {
            logger.error("Failed to send password reset OTP email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    public void sendRegistrationOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to AgriConnect — Verify Your Email");
            helper.setText(buildRegistrationOtpEmailBody(otp), true);

            mailSender.send(message);
            logger.info("Registration OTP email sent to: {}", to);

        } catch (MessagingException e) {
            logger.error("Failed to send registration OTP email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send registration OTP email. Please try again.");
        }
    }

    private String buildRegistrationOtpEmailBody(String otp) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 540px; margin: auto; padding: 32px;
                            border: 1px solid #c8e6c9; border-radius: 12px; background-color: #f9fbe7;">

                    <div style="text-align: center; margin-bottom: 24px;">
                        <h1 style="color: #2e7d32; margin: 0;">🌿 Welcome to AgriConnect!</h1>
                        <p style="color: #558b2f; font-size: 15px; margin-top: 6px;">
                            Connecting Farmers &amp; Buyers Across India
                        </p>
                    </div>

                    <p style="font-size: 15px; color: #333;">
                        Thank you for joining AgriConnect! We're excited to have you on board.
                        To complete your registration and start connecting with the agricultural
                        community, please verify your email address using the OTP below:
                    </p>

                    <div style="font-size: 36px; font-weight: bold; letter-spacing: 10px;
                                color: #1b5e20; text-align: center;
                                background-color: #e8f5e9; border-radius: 8px;
                                padding: 20px 0; margin: 24px 0;">
                        %s
                    </div>

                    <p style="color: #555; font-size: 14px;">
                        This OTP is valid for <strong>10 minutes</strong>.
                        Once verified, your account will be ready to use.
                    </p>

                    <p style="color: #555; font-size: 14px;">
                        If you did not create an account on AgriConnect, please ignore this email.
                    </p>

                    <hr style="border: none; border-top: 1px solid #c8e6c9; margin: 24px 0;">

                    <div style="text-align: center;">
                        <p style="font-size: 13px; color: #81c784; margin: 0;">
                            AgriConnect — Empowering Farmers, Enabling Growth
                        </p>
                        <p style="font-size: 11px; color: #aaa; margin: 4px 0 0;">
                            This is an automated email. Please do not reply.
                        </p>
                    </div>
                </div>
                """.formatted(otp);
    }

    private String buildPasswordResetEmailBody(String otp) {
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
