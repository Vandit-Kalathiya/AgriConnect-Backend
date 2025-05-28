package com.agriconnect.Main.Backend.DTO.User;

import jakarta.persistence.Lob;
import lombok.Data;

@Data
public class UserUpdateRequest {

    private String username;
//    private String password;
//    private String phoneNumber;
    private String address;
    @Lob
    private byte[] profilePicture;
    @Lob
    private byte[] signature;
}
