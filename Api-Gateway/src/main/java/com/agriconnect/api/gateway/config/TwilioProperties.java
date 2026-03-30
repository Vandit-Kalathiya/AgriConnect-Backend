package com.agriconnect.api.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "twilio")
@Validated
@Getter
@Setter
public class TwilioProperties {
    
    @NotBlank(message = "Twilio account SID cannot be blank")
    private String accountSid;
    
    @NotBlank(message = "Twilio auth token cannot be blank")
    private String authToken;
    
    @NotBlank(message = "Twilio phone number cannot be blank")
    private String phoneNumber;
}
