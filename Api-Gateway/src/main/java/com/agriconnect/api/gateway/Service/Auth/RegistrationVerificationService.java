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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistrationVerificationService {

    private static final long PENDING_REGISTRATION_EXPIRY_MS = 10 * 60 * 1000;

    private final ConcurrentHashMap<String, PendingRegistration> pendingStore = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final EmailOtpService emailOtpService;
    private final TwilioOtpService twilioOtpService;
    private final AuthService authService;

    @Value("${feature.mobile-verification.enabled:true}")
    private boolean mobileVerificationEnabled;

    public RegistrationVerificationService(UserRepository userRepository,
                                           EmailOtpService emailOtpService,
                                           TwilioOtpService twilioOtpService,
                                           AuthService authService) {
        this.userRepository = userRepository;
        this.emailOtpService = emailOtpService;
        this.twilioOtpService = twilioOtpService;
        this.authService = authService;
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

        emailOtpService.sendOtp(request.getEmail());
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
