package com.agriconnect.Main.Backend.Service.Twilio;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TwilioOtpService {

    Logger logger = LoggerFactory.getLogger(TwilioOtpService.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    // Using ConcurrentHashMap for thread-safe operations
    private static final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_EXPIRY_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds

    private static class OtpEntry {
        @Getter
        private final String otp;
        private final long creationTime;

        public OtpEntry(String otp) {
            this.otp = otp;
            this.creationTime = Instant.now().toEpochMilli();
        }

        public boolean isExpired() {
            return (Instant.now().toEpochMilli() - creationTime) > OTP_EXPIRY_DURATION;
        }
    }

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public String sendOtp(String toPhoneNumber) {
        String otp = generateOtp();
        String messageBody = "Your OTP is: " + otp;

        // Store OTP with timestamp
        otpStore.put(toPhoneNumber, new OtpEntry(otp));
        System.out.println(toPhoneNumber);

        Message message = Message.creator(
                new PhoneNumber("+91" + toPhoneNumber),
                new PhoneNumber(fromNumber),
                messageBody
        ).create();

        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        OtpEntry entry = otpStore.get(phoneNumber);

        if (entry == null) {
            return false;
        }

        // Check if OTP is expired
        if (entry.isExpired()) {
            otpStore.remove(phoneNumber);
            return false;
        }

        boolean isValid = entry.getOtp().equals(otp);
        // Clear OTP immediately after successful verification
        if (isValid) {
            otpStore.remove(phoneNumber);
        }

        return isValid;
    }

    public void clearOtp(String phoneNumber) {
        otpStore.remove(phoneNumber);
    }

    @Scheduled(fixedRate = 60000)
    public void cleanExpiredOtps() {
        otpStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        logger.info("Removed Otp");
    }

    public String initiateCall(String toPhoneNumber) {
        try {
            Twilio.init(accountSid, authToken);

//            String callerId="";
//            if (fromNumber != null && !fromNumber.trim().isEmpty()) {
//                callerId = "+91" + fromNumber.trim();
//            }

            String recipient = toPhoneNumber.startsWith("+") ? toPhoneNumber : "+91" + toPhoneNumber;

            Call call = Call.creator(
                            new PhoneNumber(recipient),
                            new PhoneNumber(fromNumber),
                            new com.twilio.type.Twiml("<Response><Say>Hello, this is a test call from your app!</Say></Response>"))
                    .create();

            return "Call initiated with SID: " + call.getSid();
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate call: " + e.getMessage());
        }
    }
}