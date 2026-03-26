package com.agriconnect.Main.Backend.Controller.Auth;

import com.agriconnect.Main.Backend.DTO.User.FarmerRegisterRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtResponse;
import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Service.Auth.AuthService;
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

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody FarmerRegisterRequest farmerRegisterRequest) {
        logger.info("Registration request for phone: {}", farmerRegisterRequest.getPhoneNumber());
        User user = authService.registerUser(farmerRegisterRequest);
        logger.info("User registered successfully: {}", user.getPhoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registered successfully",
                        "user", user
                ));
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
