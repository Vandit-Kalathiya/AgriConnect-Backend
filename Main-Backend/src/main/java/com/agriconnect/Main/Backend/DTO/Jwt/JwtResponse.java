package com.agriconnect.Main.Backend.DTO.Jwt;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {

	private String jwtToken;
	private String role;
}
