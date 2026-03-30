package com.agriconnect.api.gateway.Controller.Auth;

import com.agriconnect.api.gateway.DTO.Auth.ForgotPasswordRequest;
import com.agriconnect.api.gateway.DTO.Auth.ResetPasswordRequest;
import com.agriconnect.api.gateway.Service.Auth.PasswordResetService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService passwordResetService;

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Step 1 — Request OTP(s) for password reset.
     * Always sends OTP to email.
     * Also sends OTP to mobile when FEATURE_MOBILE_VERIFICATION_ENABLED=true.
     *
     * POST /auth/forgot-password
     * Body: { "email": "user@example.com" }
     * Response: { "message": "...", "mobileVerificationRequired": true/false }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Forgot password request for email: {}", request.getEmail());
        Map<String, Object> response = passwordResetService.initiateForgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2 — Verify OTP(s) and set new password.
     * Always requires emailOtp.
     * Also requires mobileOtp when FEATURE_MOBILE_VERIFICATION_ENABLED=true.
     *
     * POST /auth/reset-password
     * Body: { "email": "...", "emailOtp": "123456", "mobileOtp": "654321", "newPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        logger.info("Reset password request for email: {}", request.getEmail());
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
