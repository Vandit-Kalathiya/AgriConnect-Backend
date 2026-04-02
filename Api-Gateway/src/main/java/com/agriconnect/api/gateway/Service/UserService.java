package com.agriconnect.api.gateway.Service;

import com.agriconnect.api.gateway.DTO.User.UserUpdateRequest;
import com.agriconnect.api.gateway.Entity.User.User;
import com.agriconnect.api.gateway.Repository.User.UserRepository;
import com.agriconnect.api.gateway.kafka.NotificationEventPublisher;
import com.agriconnect.notification.avro.Priority;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${notification.topics.auth}")
    private String authTopic;

    public UserService(UserRepository userRepository,
                       NotificationEventPublisher notificationEventPublisher) {
        this.userRepository = userRepository;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.getUserByPhoneNumber(phoneNumber).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public User updateUser(String id, UserUpdateRequest userUpdateRequest, MultipartFile profilePicture, MultipartFile signatureImage) throws IOException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(userUpdateRequest.getUsername());
//        user.setPhoneNumber(userDetails.getPhoneNumber());
        user.setAddress(userUpdateRequest.getAddress());

        if (profilePicture != null && !profilePicture.isEmpty()) {
            user.setProfilePicture(profilePicture.getBytes());
        }
        if (signatureImage != null && !signatureImage.isEmpty()) {
            user.setSignature(signatureImage.getBytes());
        }

        User saved = userRepository.save(user);

        try {
            List<String> updatedFields = new ArrayList<>();
            if (userUpdateRequest.getUsername() != null) updatedFields.add("name");
            if (userUpdateRequest.getAddress() != null)  updatedFields.add("address");
            if (profilePicture != null && !profilePicture.isEmpty()) updatedFields.add("profilePicture");
            if (signatureImage != null && !signatureImage.isEmpty()) updatedFields.add("signature");

            notificationEventPublisher.publish(authTopic,
                notificationEventPublisher.buildEvent(
                    "AUTH_PROFILE_UPDATED",
                    saved.getId(),
                    "auth.profile.updated",
                    List.of("IN_APP"),
                    Map.of(
                        "updatedFields", String.join(", ", updatedFields),
                        "updatedAt",     Instant.now().toString()
                    ),
                    Priority.LOW,
                    "profile-" + saved.getId(),
                    saved.getEmail(),
                    saved.getPhoneNumber()
                )
            );
        } catch (Exception ex) {
            log.warn("[NOTIFY] Failed to publish AUTH_PROFILE_UPDATED for userId={}: {}", saved.getId(), ex.getMessage());
        }

        return saved;
    }


    public byte[] getProfileImage(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user.getProfilePicture();
    }

    public byte[] getSignature(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user.getSignature();
    }

    public User getUserByUniqueId(String id) {
        return userRepository.findUserByUniqueHexAddress(id).orElseThrow(() -> new EntityNotFoundException("User not found with unique id: " + id));
    }
}
