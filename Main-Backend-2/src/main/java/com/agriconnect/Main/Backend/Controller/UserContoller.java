package com.agriconnect.Main.Backend.Controller;

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

    @PutMapping("/update/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestPart("user") User userDetails,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "signatureImage", required = false) MultipartFile signatureImage) {
        try {
            User updatedUser = userService.updateUser(id, userDetails, profilePicture, signatureImage);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
