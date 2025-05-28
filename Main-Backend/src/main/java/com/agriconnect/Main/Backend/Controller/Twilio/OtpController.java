package com.agriconnect.Main.Backend.Controller.Twilio;

import com.agriconnect.Main.Backend.Service.Twilio.TwilioOtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/otp")
public class OtpController {

    @Autowired
    private TwilioOtpService twilioOtpService;

    @PostMapping("/send")
    public String sendOtp(@RequestParam String phoneNumber) {
        // Ensure phoneNumber includes country code (e.g., +12025550123)
        String otp = twilioOtpService.sendOtp(phoneNumber);
        return "OTP sent successfully to " + phoneNumber + ". Please check your SMS.";
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String phoneNumber, @RequestParam String otp) {
        boolean isValid = twilioOtpService.verifyOtp(phoneNumber, otp);
        if (isValid) {
            return "true";
        } else {
            return "Invalid OTP. Please try again......";
        }
    }

    @PostMapping("/initiate")
    public ResponseEntity<String> initiateCall(@RequestParam String phoneNumber) {
        try {
            String result = twilioOtpService.initiateCall(phoneNumber);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Error initiating call: " + e.getMessage());
        }
    }
}
