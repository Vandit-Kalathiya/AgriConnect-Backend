package com.agriconnect.Main.Backend.Service.Twilio;

import com.agriconnect.Main.Backend.config.TwilioProperties;
import com.agriconnect.Main.Backend.exception.BadRequestException;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TwilioOtpService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioOtpService.class);
    private static final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_EXPIRY_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds
    private static final Set<String> TEST_NUMBERS = Set.of("8780850751", "9924111980");

    private final TwilioProperties twilioProperties;
    private final Random random;

    @Autowired
    public TwilioOtpService(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
        this.random = new Random();
    }

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
        try {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
            logger.info("Twilio initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Twilio", e);
            throw new RuntimeException("Failed to initialize Twilio service", e);
        }
    }

    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public String sendOtp(String toPhoneNumber) {
        if (toPhoneNumber == null || toPhoneNumber.isEmpty()) {
            logger.error("Attempted to send OTP to empty phone number");
            throw new BadRequestException("Phone number cannot be empty");
        }

        String otp = generateOtp();
        String messageBody = "Your AgriConnect verification code is: " + otp + ". Valid for 5 minutes.";

        // Store OTP with timestamp
        otpStore.put(toPhoneNumber, new OtpEntry(otp));
        logger.info("Generated OTP for phone number: {}", toPhoneNumber);

        try {
            Message message = Message.creator(
                    new PhoneNumber("+91" + toPhoneNumber),
                    new PhoneNumber(twilioProperties.getPhoneNumber()),
                    messageBody
            ).create();

            logger.info("OTP sent successfully to {}, Message SID: {}", toPhoneNumber, message.getSid());
            return otp;

        } catch (ApiException e) {
            logger.error("Twilio API error while sending OTP to {}: {}", toPhoneNumber, e.getMessage(), e);
            otpStore.remove(toPhoneNumber); // Remove OTP if sending failed
            throw new BadRequestException("Failed to send OTP. Please check your phone number and try again.");
        } catch (Exception e) {
            logger.error("Unexpected error while sending OTP to {}: {}", toPhoneNumber, e.getMessage(), e);
            otpStore.remove(toPhoneNumber);
            throw new BadRequestException("Failed to send OTP. Please try again later.");
        }
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        if (phoneNumber == null || otp == null) {
            logger.warn("Null phone number or OTP provided for verification");
            return false;
        }

        // Test numbers bypass for development
        if (TEST_NUMBERS.contains(phoneNumber)) {
            logger.debug("Test number detected, bypassing OTP verification: {}", phoneNumber);
            return true;
        }

        OtpEntry entry = otpStore.get(phoneNumber);

        if (entry == null) {
            logger.warn("No OTP found for phone number: {}", phoneNumber);
            return false;
        }

        // Check if OTP is expired
        if (entry.isExpired()) {
            logger.warn("OTP expired for phone number: {}", phoneNumber);
            otpStore.remove(phoneNumber);
            return false;
        }

        boolean isValid = entry.getOtp().equals(otp);
        
        // Clear OTP immediately after verification attempt
        if (isValid) {
            otpStore.remove(phoneNumber);
            logger.info("OTP verified successfully for phone number: {}", phoneNumber);
        } else {
            logger.warn("Invalid OTP provided for phone number: {}", phoneNumber);
        }

        return isValid;
    }

    public void clearOtp(String phoneNumber) {
        if (phoneNumber != null) {
            otpStore.remove(phoneNumber);
            logger.debug("OTP cleared for phone number: {}", phoneNumber);
        }
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanExpiredOtps() {
        int removedCount = 0;
        for (var entry : otpStore.entrySet()) {
            if (entry.getValue().isExpired()) {
                otpStore.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            logger.info("Cleaned {} expired OTP(s)", removedCount);
        }
    }

    public String initiateCall(String toPhoneNumber) {
        if (toPhoneNumber == null || toPhoneNumber.isEmpty()) {
            logger.error("Attempted to initiate call with empty phone number");
            throw new BadRequestException("Phone number cannot be empty");
        }

        try {
            String recipient = toPhoneNumber.startsWith("+") ? toPhoneNumber : "+91" + toPhoneNumber;

            Call call = Call.creator(
                            new PhoneNumber(recipient),
                            new PhoneNumber(twilioProperties.getPhoneNumber()),
                            new com.twilio.type.Twiml("<Response><Say>Hello, this is a test call from AgriConnect!</Say></Response>"))
                    .create();

            logger.info("Call initiated successfully to {}, Call SID: {}", toPhoneNumber, call.getSid());
            return "Call initiated with SID: " + call.getSid();

        } catch (ApiException e) {
            logger.error("Twilio API error while initiating call to {}: {}", toPhoneNumber, e.getMessage(), e);
            throw new BadRequestException("Failed to initiate call. " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while initiating call to {}: {}", toPhoneNumber, e.getMessage(), e);
            throw new BadRequestException("Failed to initiate call: " + e.getMessage());
        }
    }
}