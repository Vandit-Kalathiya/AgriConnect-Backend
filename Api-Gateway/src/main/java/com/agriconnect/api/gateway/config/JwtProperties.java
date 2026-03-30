package com.agriconnect.api.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
@Getter
@Setter
public class JwtProperties {
    
    @NotBlank(message = "JWT secret cannot be blank")
    private String secret;
    
    private Long expiration = 604800000L; // 7 days default
}
