package com.agriconnect.Main.Backend.Service.Twilio;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class TwilioOtpService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    private static final Map<String, String> otpStore = new HashMap<>(); // Temporary OTP storage

    // Initialize Twilio with credentials
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    // Generate a 6-digit OTP
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generates a number between 100000 and 999999
        return String.valueOf(otp);
    }

    // Send OTP to the mobile number
    public String sendOtp(String toPhoneNumber) {
        String otp = generateOtp();
        String messageBody = "Your OTP is: " + otp;
        otpStore.put(toPhoneNumber, otp);
        System.out.println(toPhoneNumber);

        Message message = Message.creator(
                new PhoneNumber("+91"+toPhoneNumber), // Recipient's number
                new PhoneNumber(fromNumber),    // Your Twilio number
                messageBody
        ).create();

        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        String storedOtp = otpStore.get(phoneNumber);
        return storedOtp != null && storedOtp.equals(otp);
    }

    public void clearOtp(String phoneNumber) {
        otpStore.remove(phoneNumber);
    }
}
