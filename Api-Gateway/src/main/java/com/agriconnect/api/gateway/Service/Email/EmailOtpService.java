package com.agriconnect.api.gateway.Service.Email;

import com.agriconnect.api.gateway.Service.Cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailOtpService {

    private static final Logger logger = LoggerFactory.getLogger(EmailOtpService.class);
    private static final long OTP_EXPIRY_MS = 10 * 60 * 1000;
    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final EmailService emailService;
    private final CacheService cacheService;
    private final Random random = new Random();

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Autowired
    public EmailOtpService(EmailService emailService, CacheService cacheService) {
        this.emailService = emailService;
        this.cacheService = cacheService;
    }

    public void sendOtp(String email) {
        String otp = String.valueOf(100000 + random.nextInt(900000));
        String key = "otp:email:password-reset:" + email.toLowerCase();

        if (redisEnabled) {
            cacheService.save(key, new OtpEntry(otp), OTP_TTL);
        } else {
            otpStore.put(email.toLowerCase(), new OtpEntry(otp));
        }

        emailService.sendOtpEmail(email, otp);
        logger.info("Password reset OTP sent to: {} (cached: {})", email, redisEnabled);
    }

    public void sendRegistrationOtp(String email) {
        String otp = String.valueOf(100000 + random.nextInt(900000));
        String key = "otp:email:registration:" + email.toLowerCase();

        if (redisEnabled) {
            cacheService.save(key, new OtpEntry(otp), OTP_TTL);
        } else {
            otpStore.put(email.toLowerCase(), new OtpEntry(otp));
        }

        emailService.sendRegistrationOtpEmail(email, otp);
        logger.info("Registration OTP sent to: {} (cached: {})", email, redisEnabled);
    }

    public boolean verifyOtp(String email, String otp) {
        String emailKey = email.toLowerCase();
        OtpEntry entry = null;

        if (redisEnabled) {
            String passwordResetKey = "otp:email:password-reset:" + emailKey;
            String registrationKey = "otp:email:registration:" + emailKey;

            Optional<OtpEntry> passwordResetEntry = cacheService.get(passwordResetKey, OtpEntry.class);
            Optional<OtpEntry> registrationEntry = cacheService.get(registrationKey, OtpEntry.class);

            entry = passwordResetEntry.orElse(registrationEntry.orElse(null));

            if (entry != null && entry.otp.equals(otp) && !entry.isExpired() && !entry.isUsed()) {
                cacheService.evict(passwordResetKey);
                cacheService.evict(registrationKey);
                logger.info("Email OTP verified successfully for: {} (from cache)", email);
                return true;
            }
        } else {
            entry = otpStore.get(emailKey);
        }

        if (entry == null) {
            logger.warn("No email OTP found for: {}", email);
            return false;
        }
        if (entry.isExpired()) {
            if (!redisEnabled)
                otpStore.remove(emailKey);
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
        if (!redisEnabled)
            otpStore.remove(emailKey);
        logger.info("Email OTP verified successfully for: {}", email);
        return true;
    }

    public void clearOtp(String email) {
        String emailKey = email.toLowerCase();

        if (redisEnabled) {
            cacheService.evict("otp:email:password-reset:" + emailKey);
            cacheService.evict("otp:email:registration:" + emailKey);
        } else {
            otpStore.remove(emailKey);
        }
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
        if (removed > 0)
            logger.info("Cleaned {} expired/used email OTP(s)", removed);
    }

    public static class OtpEntry implements Serializable {
        private String otp;
        private long createdAt;
        private boolean used;

        public OtpEntry() {
        }

        public OtpEntry(String otp) {
            this.otp = otp;
            this.createdAt = Instant.now().toEpochMilli();
            this.used = false;
        }

        public String getOtp() {
            return otp;
        }

        public void setOtp(String otp) {
            this.otp = otp;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public boolean isUsed() {
            return used;
        }

        public void setUsed(boolean used) {
            this.used = used;
        }

        boolean isExpired() {
            return (Instant.now().toEpochMilli() - createdAt) > OTP_EXPIRY_MS;
        }

        void markUsed() {
            this.used = true;
        }
    }
}
