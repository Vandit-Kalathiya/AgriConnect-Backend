package com.agriconnect.Main.Backend.Controller.Twilio;

import com.agriconnect.Main.Backend.Service.Twilio.TwilioOtpService;
import org.springframework.beans.factory.annotation.Autowired;
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
}
