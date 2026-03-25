package com.agriconnect.Main.Backend.Service;

import com.agriconnect.Main.Backend.DTO.User.UserUpdateRequest;
import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Repository.User.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        return userRepository.save(user);
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
