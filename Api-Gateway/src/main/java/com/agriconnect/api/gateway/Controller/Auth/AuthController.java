package com.agriconnect.api.gateway.Controller.Auth;

import com.agriconnect.api.gateway.DTO.User.FarmerRegisterRequest;
import com.agriconnect.api.gateway.DTO.Auth.RegisterVerificationRequest;
import com.agriconnect.api.gateway.DTO.Jwt.JwtRequest;
import com.agriconnect.api.gateway.DTO.Jwt.JwtResponse;
import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Service.Auth.AuthService;
import com.agriconnect.api.gateway.Service.Auth.RegistrationVerificationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final RegistrationVerificationService registrationVerificationService;

    @Autowired
    public AuthController(AuthService authService, RegistrationVerificationService registrationVerificationService) {
        this.authService = authService;
        this.registrationVerificationService = registrationVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody FarmerRegisterRequest farmerRegisterRequest) {
        logger.info("Registration OTP request for phone: {}", farmerRegisterRequest.getPhoneNumber());
        Map<String, Object> response = registrationVerificationService.initiateRegistration(farmerRegisterRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/verify")
    public ResponseEntity<Map<String, Object>> verifyAndRegister(
            @Valid @RequestBody RegisterVerificationRequest request) {
        logger.info("Registration OTP verification for email: {}", request.getEmail());
        User user = registrationVerificationService.verifyAndRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registered successfully", "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody JwtRequest jwtRequest,
            HttpServletResponse response) {
        logger.info("Login request for phone: {}", jwtRequest.getPhoneNumber());
        JwtResponse jwtResponse = authService.login(jwtRequest, response);
        return ResponseEntity.ok(jwtResponse);
    }

    @GetMapping("/current-user")
    public ResponseEntity<User> getCurrentUser() {
        logger.debug("Fetching current authenticated user");
        User currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(currentUser);
    }
}
