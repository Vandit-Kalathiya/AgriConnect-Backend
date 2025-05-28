package com.agriconnect.Main.Backend.Controller.Auth;

import com.agriconnect.Main.Backend.DTO.User.FarmerRegisterRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtRequest;
import com.agriconnect.Main.Backend.DTO.Jwt.JwtResponse;
import com.agriconnect.Main.Backend.Service.Auth.AuthService;
import com.agriconnect.Main.Backend.Service.Twilio.TwilioOtpService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @Autowired
    private TwilioOtpService twilioOtpService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody FarmerRegisterRequest farmerRegisterRequest) {
        try {
            authService.sendRegisterOtp(farmerRegisterRequest.getPhoneNumber());
            return new ResponseEntity<>("Otp Sent Successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login/after/register")
    public ResponseEntity<?> loginAfterRegister(@RequestBody JwtRequest jwtRequest, HttpServletResponse response) {
        try {
            return new ResponseEntity<>(authService.login(jwtRequest, response), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JwtRequest jwtRequest, HttpServletResponse response) {
        try {
            authService.sendLoginOtp(jwtRequest.getPhoneNumber());
            return new ResponseEntity<>("Otp Sent Successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/r/verify-otp/{mobileNumber}/{otp}")
    public ResponseEntity<?> verifyOtpAndRegister(@PathVariable String mobileNumber, @PathVariable String otp, @RequestBody FarmerRegisterRequest farmerRegisterRequest, HttpServletResponse response) {
        try {
            if (!twilioOtpService.verifyOtp(mobileNumber, otp)) {
                throw new RuntimeException("Invalid OTP");
            }
            return new ResponseEntity<>("Registered Successfully.: " + authService.registerUser(farmerRegisterRequest), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verify-otp/{mobileNumber}/{otp}")
    public ResponseEntity<?> verifyOtpAndLogin(@PathVariable String mobileNumber, @PathVariable String otp, HttpServletResponse response) {
        try {
            JwtResponse jwtResponse = authService.verifyAndLogin(mobileNumber, otp, response);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Object currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return new ResponseEntity<>("No authenticated user found", HttpStatus.UNAUTHORIZED);
            }
            return ResponseEntity.ok(currentUser);
        } catch (AuthenticationException e) {
            // Handle authentication-specific exceptions (e.g., user not logged in)
            return new ResponseEntity<>("Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (SecurityException e) {
            // Handle security-related exceptions (e.g., insufficient permissions)
            return new ResponseEntity<>("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            return new ResponseEntity<>("An unexpected error occurred: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @GetMapping("/hello")
//    public ResponseEntity<String> getHello() {
//        Object getFaculty = userRepository.findAll();
//        return ResponseEntity.ok("Hello from Spring Boot!" + getFaculty);
//    }
}
