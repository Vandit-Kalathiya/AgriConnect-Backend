package com.agriconnect.Main.Backend.Service.Auth;

import com.agriconnect.Main.Backend.DTO.Auth.ForgotPasswordRequest;
import com.agriconnect.Main.Backend.DTO.Auth.ResetPasswordRequest;
import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Repository.User.UserRepository;
import com.agriconnect.Main.Backend.Service.Email.EmailOtpService;
import com.agriconnect.Main.Backend.Service.Twilio.TwilioOtpService;
import com.agriconnect.Main.Backend.exception.BadRequestException;
import com.agriconnect.Main.Backend.exception.ResourceNotFoundException;
import com.agriconnect.Main.Backend.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final EmailOtpService emailOtpService;
    private final TwilioOtpService twilioOtpService;
    private final PasswordEncoder passwordEncoder;

    @Value("${feature.mobile-verification.enabled:true}")
    private boolean mobileVerificationEnabled;

    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                EmailOtpService emailOtpService,
                                TwilioOtpService twilioOtpService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailOtpService = emailOtpService;
        this.twilioOtpService = twilioOtpService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Step 1 — Initiate password reset.
     * Always sends an OTP to the user's email.
     * When mobile verification is enabled, also sends an OTP via SMS.
     */
    public Map<String, Object> initiateForgotPassword(ForgotPasswordRequest request) {
        logger.info("Password reset initiated for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        emailOtpService.sendOtp(request.getEmail());
        logger.info("Email OTP sent to: {}", request.getEmail());

        if (mobileVerificationEnabled) {
            twilioOtpService.sendOtp(user.getPhoneNumber());
            logger.info("Mobile OTP sent to: {}", user.getPhoneNumber());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", mobileVerificationEnabled
                ? "OTP sent to your email and mobile number"
                : "OTP sent to your email");
        response.put("mobileVerificationRequired", mobileVerificationEnabled);
        return response;
    }

    /**
     * Step 2 — Verify OTPs and set new password.
     * Always verifies email OTP.
     * When mobile verification is enabled, also verifies mobile OTP.
     */
    public void resetPassword(ResetPasswordRequest request) {
        logger.info("Password reset verification for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!emailOtpService.verifyOtp(request.getEmail(), request.getEmailOtp())) {
            throw new UnauthorizedException("Invalid or expired email OTP");
        }

        if (mobileVerificationEnabled) {
            if (request.getMobileOtp() == null || request.getMobileOtp().isBlank()) {
                throw new BadRequestException("Mobile OTP is required when mobile verification is enabled");
            }
            if (!twilioOtpService.verifyOtp(user.getPhoneNumber(), request.getMobileOtp())) {
                throw new UnauthorizedException("Invalid or expired mobile OTP");
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Password reset successfully for email: {}", request.getEmail());
    }
}
