package com.agriconnect.api.gateway.Service.Auth;

import com.agriconnect.api.gateway.DTO.Auth.RegisterVerificationRequest;
import com.agriconnect.api.gateway.DTO.User.FarmerRegisterRequest;
import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Repository.User.UserRepository;
import com.agriconnect.api.gateway.Service.Email.EmailOtpService;
import com.agriconnect.api.gateway.Service.Twilio.TwilioOtpService;
import com.agriconnect.api.gateway.exception.BadRequestException;
import com.agriconnect.api.gateway.exception.ConflictException;
import com.agriconnect.api.gateway.exception.UnauthorizedException;
import com.agriconnect.api.gateway.kafka.NotificationEventPublisher;
import com.agriconnect.notification.avro.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistrationVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationVerificationService.class);
    private static final long PENDING_REGISTRATION_EXPIRY_MS = 10 * 60 * 1000;

    private final ConcurrentHashMap<String, PendingRegistration> pendingStore = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final EmailOtpService emailOtpService;
    private final TwilioOtpService twilioOtpService;
    private final AuthService authService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${feature.mobile-verification.enabled:true}")
    private boolean mobileVerificationEnabled;

    @Value("${notification.topics.auth}")
    private String authTopic;

    public RegistrationVerificationService(UserRepository userRepository,
                                           EmailOtpService emailOtpService,
                                           TwilioOtpService twilioOtpService,
                                           AuthService authService,
                                           NotificationEventPublisher notificationEventPublisher) {
        this.userRepository = userRepository;
        this.emailOtpService = emailOtpService;
        this.twilioOtpService = twilioOtpService;
        this.authService = authService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    public Map<String, Object> initiateRegistration(FarmerRegisterRequest request) {
        if (userRepository.getUserByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new ConflictException("Phone number already registered!");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered!");
        }

        String emailKey = request.getEmail().toLowerCase();
        pendingStore.put(emailKey, new PendingRegistration(request));

        emailOtpService.sendRegistrationOtp(request.getEmail());
        if (mobileVerificationEnabled) {
            twilioOtpService.sendOtp(request.getPhoneNumber());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", mobileVerificationEnabled
                ? "OTP sent to your email and mobile number"
                : "OTP sent to your email");
        response.put("mobileVerificationRequired", mobileVerificationEnabled);
        return response;
    }

    public User verifyAndRegister(RegisterVerificationRequest request) {
        String emailKey = request.getEmail().toLowerCase();
        PendingRegistration pending = pendingStore.get(emailKey);
        if (pending == null || pending.isExpired()) {
            pendingStore.remove(emailKey);
            throw new BadRequestException("Registration session expired. Please register again.");
        }

        if (!emailOtpService.verifyOtp(request.getEmail(), request.getEmailOtp())) {
            throw new UnauthorizedException("Invalid or expired email OTP");
        }

        if (mobileVerificationEnabled) {
            if (request.getMobileOtp() == null || request.getMobileOtp().isBlank()) {
                throw new BadRequestException("Mobile OTP is required");
            }
            if (!twilioOtpService.verifyOtp(pending.request.getPhoneNumber(), request.getMobileOtp())) {
                throw new UnauthorizedException("Invalid or expired mobile OTP");
            }
        }

        User user = authService.registerUser(pending.request);
        pendingStore.remove(emailKey);

        try {
            notificationEventPublisher.publish(authTopic,
                notificationEventPublisher.buildEvent(
                    "AUTH_WELCOME",
                    user.getId(),
                    "auth.welcome",
                    List.of("EMAIL", "IN_APP"),
                    Map.of(
                        "userName",      user.getUsername() != null ? user.getUsername() : "Farmer",
                        "userPhone",     user.getPhoneNumber(),
                        "registeredAt",  Instant.now().toString()
                    ),
                    Priority.NORMAL,
                    user.getId(),
                    user.getEmail(),
                    user.getPhoneNumber()
                )
            );
        } catch (Exception ex) {
            log.warn("[NOTIFY] Failed to publish AUTH_WELCOME for userId={}: {}", user.getId(), ex.getMessage());
        }

        return user;
    }

    private static class PendingRegistration {
        private final FarmerRegisterRequest request;
        private final long createdAt;

        private PendingRegistration(FarmerRegisterRequest request) {
            this.request = request;
            this.createdAt = Instant.now().toEpochMilli();
        }

        private boolean isExpired() {
            return (Instant.now().toEpochMilli() - createdAt) > PENDING_REGISTRATION_EXPIRY_MS;
        }
    }
}
