package com.agriconnect.api.gateway.Controller.Auth;

import com.agriconnect.api.gateway.DTO.User.FarmerRegisterRequest;
import com.agriconnect.api.gateway.DTO.Auth.RegisterVerificationRequest;
import com.agriconnect.api.gateway.DTO.Jwt.JwtRequest;
import com.agriconnect.api.gateway.DTO.Jwt.JwtResponse;
import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Service.Auth.AuthService;
import com.agriconnect.api.gateway.Service.Auth.RegistrationVerificationService;
import com.agriconnect.api.gateway.Service.Cache.CacheService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final RegistrationVerificationService registrationVerificationService;
    private final CacheService cacheService;

    @Autowired
    public AuthController(AuthService authService, RegistrationVerificationService registrationVerificationService, CacheService cacheService) {
        this.authService = authService;
        this.registrationVerificationService = registrationVerificationService;
        this.cacheService = cacheService;
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
        logger.info("Login request for username: {}", jwtRequest.getUsername());
        JwtResponse jwtResponse = authService.login(jwtRequest, response);
        return ResponseEntity.ok(jwtResponse);
    }

    @GetMapping("/current-user")
    public ResponseEntity<User> getCurrentUser() {
        logger.debug("Fetching current authenticated user");
        User currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/test-cache")
    public ResponseEntity<Map<String, String>> testCache() {
        // Save to cache
        cacheService.save("test:user:123", "John Doe", Duration.ofMinutes(5));

        // Get from cache
        Optional<String> value = cacheService.get("test:user:123", String.class);

        // Check existence
        boolean exists = cacheService.exists("test:user:123");

        return ResponseEntity.ok(Map.of(
                "saved", "test:user:123",
                "retrieved", value.orElse("NOT FOUND"),
                "exists", String.valueOf(exists)
        ));
    }
}
