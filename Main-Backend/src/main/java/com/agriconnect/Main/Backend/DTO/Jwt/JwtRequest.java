package com.agriconnect.Main.Backend.DTO.Jwt;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtRequest {
	private String phoneNumber;
}
