package com.agriconnect.api.gateway.Service.Email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailOtpService {

    private static final Logger logger = LoggerFactory.getLogger(EmailOtpService.class);
    private static final long OTP_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes

    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final EmailService emailService;
    private final Random random = new Random();

    @Autowired
    public EmailOtpService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendOtp(String email) {
        String otp = String.valueOf(100000 + random.nextInt(900000));
        otpStore.put(email.toLowerCase(), new OtpEntry(otp));
        emailService.sendOtpEmail(email, otp);
        logger.info("Password reset OTP sent to: {}", email);
    }

    public void sendRegistrationOtp(String email) {
        String otp = String.valueOf(100000 + random.nextInt(900000));
        otpStore.put(email.toLowerCase(), new OtpEntry(otp));
        emailService.sendRegistrationOtpEmail(email, otp);
        logger.info("Registration OTP sent to: {}", email);
    }

    public boolean verifyOtp(String email, String otp) {
        String key = email.toLowerCase();
        OtpEntry entry = otpStore.get(key);

        if (entry == null) {
            logger.warn("No email OTP found for: {}", email);
            return false;
        }
        if (entry.isExpired()) {
            otpStore.remove(key);
            logger.warn("Email OTP expired for: {}", email);
            return false;
        }
        if (entry.isUsed()) {
            logger.warn("Email OTP already used for: {}", email);
            return false;
        }
        if (!entry.otp.equals(otp)) {
            logger.warn("Invalid email OTP for: {}", email);
            return false;
        }

        entry.markUsed();
        logger.info("Email OTP verified successfully for: {}", email);
        return true;
    }

    public void clearOtp(String email) {
        otpStore.remove(email.toLowerCase());
    }

    @Scheduled(fixedRate = 60000)
    public void cleanExpiredOtps() {
        long now = Instant.now().toEpochMilli();
        int removed = 0;
        for (var e : otpStore.entrySet()) {
            OtpEntry entry = e.getValue();
            if (entry.isExpired() || (entry.isUsed() && (now - entry.createdAt) > 10_000)) {
                otpStore.remove(e.getKey());
                removed++;
            }
        }
        if (removed > 0) logger.info("Cleaned {} expired/used email OTP(s)", removed);
    }

    private static class OtpEntry {
        final String otp;
        final long createdAt;
        private boolean used;

        OtpEntry(String otp) {
            this.otp = otp;
            this.createdAt = Instant.now().toEpochMilli();
            this.used = false;
        }

        boolean isExpired() {
            return (Instant.now().toEpochMilli() - createdAt) > OTP_EXPIRY_MS;
        }

        boolean isUsed() { return used; }

        void markUsed() { this.used = true; }
    }
}
