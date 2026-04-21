package com.agriconnect.api.gateway.Service;

import com.agriconnect.api.gateway.DTO.User.UserUpdateRequest;
import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Repository.User.UserRepository;
import com.agriconnect.api.gateway.Service.Cache.CacheService;
import com.agriconnect.api.gateway.kafka.NotificationEventPublisher;
import com.agriconnect.notification.avro.Priority;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String USER_PHONE_KEY_PREFIX = "user:phone:";
    private static final String USER_ID_KEY_PREFIX = "user:id:";
    private static final String USER_HEX_KEY_PREFIX = "user:hex:";
    private static final String USER_PROFILE_IMG_PREFIX = "user:profile:img:";
    private static final String USER_SIGNATURE_PREFIX = "user:signature:";

    private static final Duration USER_TTL = Duration.ofHours(12);
    private static final Duration IMAGE_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final CacheService cacheService;

    @Value("${notification.topics.auth}")
    private String authTopic;

    public UserService(UserRepository userRepository,
            NotificationEventPublisher notificationEventPublisher,
            CacheService cacheService) {
        this.userRepository = userRepository;
        this.notificationEventPublisher = notificationEventPublisher;
        this.cacheService = cacheService;
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        String cacheKey = USER_PHONE_KEY_PREFIX + phoneNumber;
        return cacheService.get(cacheKey, User.class).orElseGet(() -> {
            User user = userRepository.getUserByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            cacheService.save(cacheKey, user, USER_TTL);
            log.debug("Cached user by phone: {}", phoneNumber);
            return user;
        });
    }

    public User updateUser(String id, UserUpdateRequest userUpdateRequest, MultipartFile profilePicture,
            MultipartFile signatureImage) throws IOException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(userUpdateRequest.getUsername());
        // user.setPhoneNumber(userDetails.getPhoneNumber());
        user.setAddress(userUpdateRequest.getAddress());

        if (profilePicture != null && !profilePicture.isEmpty()) {
            user.setProfilePicture(profilePicture.getBytes());
        }
        if (signatureImage != null && !signatureImage.isEmpty()) {
            user.setSignature(signatureImage.getBytes());
        }

        User saved = userRepository.save(user);

        // Evict all user caches
        cacheService.evict(USER_PHONE_KEY_PREFIX + saved.getPhoneNumber());
        cacheService.evict(USER_ID_KEY_PREFIX + saved.getId());
        cacheService.evict(USER_HEX_KEY_PREFIX + saved.getUniqueHexAddress());
        cacheService.evict(USER_PROFILE_IMG_PREFIX + saved.getId());
        cacheService.evict(USER_SIGNATURE_PREFIX + saved.getId());
        log.debug("Evicted user caches for userId: {}", saved.getId());

        try {
            List<String> updatedFields = new ArrayList<>();
            if (userUpdateRequest.getUsername() != null)
                updatedFields.add("name");
            if (userUpdateRequest.getAddress() != null)
                updatedFields.add("address");
            if (profilePicture != null && !profilePicture.isEmpty())
                updatedFields.add("profilePicture");
            if (signatureImage != null && !signatureImage.isEmpty())
                updatedFields.add("signature");

            notificationEventPublisher.publish(authTopic,
                    notificationEventPublisher.buildEvent(
                            "AUTH_PROFILE_UPDATED",
                            saved.getId(),
                            "auth.profile.updated",
                            List.of("IN_APP"),
                            Map.of(
                                    "updatedFields", String.join(", ", updatedFields),
                                    "updatedAt", Instant.now().toString()),
                            Priority.LOW,
                            "profile-" + saved.getId(),
                            saved.getEmail(),
                            saved.getPhoneNumber()));
        } catch (Exception ex) {
            log.warn("[NOTIFY] Failed to publish AUTH_PROFILE_UPDATED for userId={}: {}", saved.getId(),
                    ex.getMessage());
        }

        return saved;
    }

    public byte[] getProfileImage(String userId) {
        String cacheKey = USER_PROFILE_IMG_PREFIX + userId;
        byte[] cached = cacheService.get(cacheKey, byte[].class).orElse(null);
        if (cached != null) {
            log.debug("Cache HIT for profile image: {}", userId);
            return cached;
        }

        log.debug("Cache MISS for profile image: {}, fetching from DB", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        byte[] profilePicture = user.getProfilePicture();
        if (profilePicture != null) {
            cacheService.save(cacheKey, profilePicture, IMAGE_TTL);
            log.debug("Cached profile image for userId: {}", userId);
        }
        return profilePicture;
    }

    public byte[] getSignature(String userId) {
        String cacheKey = USER_SIGNATURE_PREFIX + userId;
        byte[] cached = cacheService.get(cacheKey, byte[].class).orElse(null);
        if (cached != null) {
            log.debug("Cache HIT for signature: {}", userId);
            return cached;
        }

        log.debug("Cache MISS for signature: {}, fetching from DB", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        byte[] signature = user.getSignature();
        if (signature != null) {
            cacheService.save(cacheKey, signature, IMAGE_TTL);
            log.debug("Cached signature for userId: {}", userId);
        }
        return signature;
    }

    public User getUserByUniqueId(String id) {
        String cacheKey = USER_HEX_KEY_PREFIX + id;
        return cacheService.get(cacheKey, User.class).orElseGet(() -> {
            User user = userRepository.findUserByUniqueHexAddress(id)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with unique id: " + id));
            cacheService.save(cacheKey, user, USER_TTL);
            log.debug("Cached user by hex address: {}", id);
            return user;
        });
    }
}
