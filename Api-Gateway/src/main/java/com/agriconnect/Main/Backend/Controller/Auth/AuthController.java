package com.agriconnect.Main.Backend.Controller.Auth;

import com.agriconnect.Main.Backend.DTO.User.FarmerRegisterRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtResponse;
import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Service.Auth.AuthService;
import com.agriconnect.Main.Backend.Service.Twilio.TwilioOtpService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
    private final TwilioOtpService twilioOtpService;

    @Autowired
    public AuthController(AuthService authService, TwilioOtpService twilioOtpService) {
        this.authService = authService;
        this.twilioOtpService = twilioOtpService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody FarmerRegisterRequest farmerRegisterRequest) {
        logger.info("Registration OTP request for phone: {}", farmerRegisterRequest.getPhoneNumber());
        authService.sendRegisterOtp(farmerRegisterRequest.getPhoneNumber());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/login/after/register")
    public ResponseEntity<JwtResponse> loginAfterRegister(@Valid @RequestBody JwtRequest jwtRequest, HttpServletResponse response) {
        logger.info("Login after registration for phone: {}", jwtRequest.getPhoneNumber());
        JwtResponse jwtResponse = authService.login(jwtRequest, response);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody JwtRequest jwtRequest) {
        logger.info("Login OTP request for phone: {}", jwtRequest.getPhoneNumber());
        authService.sendLoginOtp(jwtRequest.getPhoneNumber());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/r/verify-otp/{mobileNumber}/{otp}")
    public ResponseEntity<Map<String, Object>> verifyOtpAndRegister(
            @PathVariable @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits") String mobileNumber,
            @PathVariable @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits") String otp,
            @Valid @RequestBody FarmerRegisterRequest farmerRegisterRequest,
            HttpServletResponse response) {
        
        logger.info("Verify OTP and register for phone: {}", mobileNumber);
        
        if (!twilioOtpService.verifyOtp(mobileNumber, otp)) {
            logger.warn("Invalid OTP provided for registration: {}", mobileNumber);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired OTP"));
        }
        
        User user = authService.registerUser(farmerRegisterRequest);
        logger.info("User registered successfully: {}", user.getPhoneNumber());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "Registered successfully",
                    "user", user
                ));
    }

    @PostMapping("/verify-otp/{mobileNumber}/{otp}")
    public ResponseEntity<JwtResponse> verifyOtpAndLogin(
            @PathVariable @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits") String mobileNumber,
            @PathVariable @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits") String otp,
            HttpServletResponse response) {
        
        logger.info("Verify OTP and login for phone: {}", mobileNumber);
        JwtResponse jwtResponse = authService.verifyAndLogin(mobileNumber, otp, response);
        return ResponseEntity.ok(jwtResponse);
    }

    @GetMapping("/current-user")
    public ResponseEntity<User> getCurrentUser() {
        logger.debug("Fetching current authenticated user");
        User currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(currentUser);
    }
}
