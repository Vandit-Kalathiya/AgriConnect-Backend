package com.agriconnect.api.gateway.Service.Auth;

import com.agriconnect.api.gateway.DTO.Auth.ForgotPasswordRequest;
import com.agriconnect.api.gateway.DTO.Auth.ResetPasswordRequest;
import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Repository.User.UserRepository;
import com.agriconnect.api.gateway.Service.Email.EmailOtpService;
import com.agriconnect.api.gateway.exception.ResourceNotFoundException;
import com.agriconnect.api.gateway.exception.UnauthorizedException;
import com.agriconnect.api.gateway.kafka.NotificationEventPublisher;
import com.agriconnect.notification.avro.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final EmailOtpService emailOtpService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${notification.topics.auth}")
    private String authTopic;

    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                EmailOtpService emailOtpService,
                                PasswordEncoder passwordEncoder,
                                NotificationEventPublisher notificationEventPublisher) {
        this.userRepository = userRepository;
        this.emailOtpService = emailOtpService;
        this.passwordEncoder = passwordEncoder;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    /**
     * Step 1 — Initiate password reset (email OTP only).
     */
    public Map<String, Object> initiateForgotPassword(ForgotPasswordRequest request) {
        logger.info("Password reset initiated for email: {}", request.getEmail());

        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        emailOtpService.sendOtp(request.getEmail());
        logger.info("Email OTP sent to: {}", request.getEmail());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "OTP sent to your email");
        response.put("mobileVerificationRequired", false);
        return response;
    }

    /**
     * Step 2 — Verify email OTP and set new password.
     */
    public void resetPassword(ResetPasswordRequest request) {
        logger.info("Password reset verification for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!emailOtpService.verifyOtp(request.getEmail(), request.getEmailOtp())) {
            throw new UnauthorizedException("Invalid or expired email OTP");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        try {
            notificationEventPublisher.publish(authTopic,
                notificationEventPublisher.buildEvent(
                    "AUTH_PASSWORD_RESET_SUCCESS",
                    user.getId(),
                    "auth.password.reset.success",
                    List.of("EMAIL", "IN_APP"),
                    Map.of(
                        "userName", user.getUsername() != null ? user.getUsername() : "Farmer",
                        "resetAt",  Instant.now().toString()
                    ),
                    Priority.HIGH,
                    "pwd-reset-" + user.getId(),
                    user.getEmail(),
                    user.getPhoneNumber()
                )
            );
        } catch (Exception ex) {
            logger.warn("[NOTIFY] Failed to publish AUTH_PASSWORD_RESET_SUCCESS: {}", ex.getMessage());
        }

        logger.info("Password reset successfully for email: {}", request.getEmail());
    }
}
