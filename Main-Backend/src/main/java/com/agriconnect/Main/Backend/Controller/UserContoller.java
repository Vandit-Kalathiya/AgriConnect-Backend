package com.agriconnect.Main.Backend.Controller;

import com.agriconnect.Main.Backend.DTO.User.UserUpdateRequest;
import com.agriconnect.Main.Backend.Entity.User.User;
import com.agriconnect.Main.Backend.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserContoller {

    private final UserService userService;

    public UserContoller(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("{phone}")
    public ResponseEntity<?> getUserByPhone(@PathVariable String phone) {
        try{
            return new ResponseEntity<>(userService.getUserByPhoneNumber(phone), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get User by unique ID
    @GetMapping("/unique/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try{
            return new ResponseEntity<>(userService.getUserByUniqueId(id), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestPart("userUpdateRequest") UserUpdateRequest userUpdateRequest,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "signatureImage", required = false) MultipartFile signatureImage) {
        try {
            User updatedUser = userService.updateUser(id, userUpdateRequest, profilePicture, signatureImage);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }


    @GetMapping("/profile-image/{id}")
    public ResponseEntity<?> getUserProfileImage(@PathVariable String id) {
        try {
            byte[] profileImage = userService.getProfileImage(id);
            return ResponseEntity.ok(profileImage);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/signature-image/{id}")
    public ResponseEntity<?> getUserSignatureImage(@PathVariable String id) {
        try {
            byte[] signatureImage = userService.getSignature(id);
            return ResponseEntity.ok(signatureImage);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
