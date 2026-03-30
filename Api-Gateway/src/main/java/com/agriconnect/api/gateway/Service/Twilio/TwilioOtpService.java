package com.agriconnect.api.gateway.Service.Twilio;

import com.agriconnect.api.gateway.config.TwilioProperties;
import com.agriconnect.api.gateway.exception.BadRequestException;
import com.agriconnect.api.gateway.util.PhoneNumberUtil;
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
        @Getter
        private boolean used;

        public OtpEntry(String otp) {
            this.otp = otp;
            this.creationTime = Instant.now().toEpochMilli();
            this.used = false;
        }

        public boolean isExpired() {
            return (Instant.now().toEpochMilli() - creationTime) > OTP_EXPIRY_DURATION;
        }
        
        public void markAsUsed() {
            this.used = true;
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

        // Normalize phone number to 10-digit format
        String normalizedNumber = PhoneNumberUtil.normalize(toPhoneNumber);
        logger.debug("Normalized phone number: {} -> {}", toPhoneNumber, normalizedNumber);

        String otp = generateOtp();
        String messageBody = "Your AgriConnect verification code is: " + otp + ". Valid for 5 minutes.";

        // Store OTP with normalized number as key
        otpStore.put(normalizedNumber, new OtpEntry(otp));
        logger.info("Generated and stored OTP for normalized phone number: {}", normalizedNumber);

        try {
            // Format number for Twilio (+91 prefix)
            String twilioFormattedNumber = PhoneNumberUtil.formatForTwilio(normalizedNumber);
            
            Message message = Message.creator(
                    new PhoneNumber(twilioFormattedNumber),
                    new PhoneNumber(twilioProperties.getPhoneNumber()),
                    messageBody
            ).create();

            logger.info("OTP sent successfully to {}, Message SID: {}", normalizedNumber, message.getSid());
            return otp;

        } catch (ApiException e) {
            logger.error("Twilio API error while sending OTP to {}: {}", normalizedNumber, e.getMessage(), e);
            otpStore.remove(normalizedNumber); // Remove OTP if sending failed
            throw new BadRequestException("Failed to send OTP. Please check your phone number and try again.");
        } catch (Exception e) {
            logger.error("Unexpected error while sending OTP to {}: {}", normalizedNumber, e.getMessage(), e);
            otpStore.remove(normalizedNumber);
            throw new BadRequestException("Failed to send OTP. Please try again later.");
        }
    }

    public boolean verifyOtp(String phoneNumber, String otp) {
        if (phoneNumber == null || otp == null) {
            logger.warn("Null phone number or OTP provided for verification");
            return false;
        }

        // Normalize phone number to ensure consistency
        String normalizedNumber = PhoneNumberUtil.normalize(phoneNumber);
        logger.debug("Verifying OTP for normalized phone number: {} (original: {})", normalizedNumber, phoneNumber);

        // Test numbers bypass for development
        if (TEST_NUMBERS.contains(normalizedNumber)) {
            logger.debug("Test number detected, bypassing OTP verification: {}", normalizedNumber);
            return true;
        }

        OtpEntry entry = otpStore.get(normalizedNumber);

        if (entry == null) {
            logger.warn("No OTP found for phone number: {} (normalized: {}). OTP may have expired or already been used.", 
                       phoneNumber, normalizedNumber);
            logger.debug("Current OTP store keys: {}", otpStore.keySet());
            return false;
        }

        // Check if OTP was already used (for idempotency)
        if (entry.isUsed()) {
            logger.warn("OTP already used for phone number: {}. Preventing duplicate verification.", normalizedNumber);
            return false;
        }

        // Check if OTP is expired
        if (entry.isExpired()) {
            logger.warn("OTP expired for phone number: {}", normalizedNumber);
            otpStore.remove(normalizedNumber);
            return false;
        }

        boolean isValid = entry.getOtp().equals(otp);
        
        if (isValid) {
            // Mark as used but keep in store briefly to handle duplicate requests
            entry.markAsUsed();
            logger.info("OTP verified successfully for phone number: {}. Marked as used.", normalizedNumber);
            
            // Remove after a short delay (will be cleaned up by scheduled task)
            // This prevents duplicate verification within the same second
        } else {
            logger.warn("Invalid OTP provided for phone number: {} (expected: {}, got: {})", 
                       normalizedNumber, entry.getOtp(), otp);
        }

        return isValid;
    }

    public void clearOtp(String phoneNumber) {
        if (phoneNumber != null) {
            String normalizedNumber = PhoneNumberUtil.normalize(phoneNumber);
            otpStore.remove(normalizedNumber);
            logger.debug("OTP cleared for phone number: {}", normalizedNumber);
        }
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanExpiredOtps() {
        int removedCount = 0;
        long now = Instant.now().toEpochMilli();
        
        for (var entry : otpStore.entrySet()) {
            OtpEntry otpEntry = entry.getValue();
            
            // Remove if expired OR if used and older than 10 seconds
            boolean shouldRemove = otpEntry.isExpired() || 
                                  (otpEntry.isUsed() && (now - otpEntry.creationTime) > 10000);
            
            if (shouldRemove) {
                otpStore.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned {} expired/used OTP(s)", removedCount);
        }
    }

    public String initiateCall(String toPhoneNumber) {
        if (toPhoneNumber == null || toPhoneNumber.isEmpty()) {
            logger.error("Attempted to initiate call with empty phone number");
            throw new BadRequestException("Phone number cannot be empty");
        }

        try {
            String twilioFormattedNumber = PhoneNumberUtil.formatForTwilio(toPhoneNumber);

            Call call = Call.creator(
                            new PhoneNumber(twilioFormattedNumber),
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