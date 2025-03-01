package com.agriconnect.Main.Backend.DTO.Farmer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FarmerRegisterRequest {

    private String username;
    private String phoneNumber;
    private String address;
}
