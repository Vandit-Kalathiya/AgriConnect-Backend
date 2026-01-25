package com.agriconnect.Contract.Farming.App.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "razorpay")
@Validated
@Getter
@Setter
public class RazorpayProperties {
    
    private Key key = new Key();
    private String currency = "INR";
    private Company company = new Company();
    
    @Getter
    @Setter
    public static class Key {
        @NotBlank(message = "Razorpay key ID cannot be blank")
        private String id;
        
        @NotBlank(message = "Razorpay key secret cannot be blank")
        private String secret;
    }
    
    @Getter
    @Setter
    public static class Company {
        private String name = "AgriConnect";
    }
}
