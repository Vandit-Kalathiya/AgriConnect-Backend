package com.agriconnect.Contract.Farming.App.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "blockchain")
@Validated
@Getter
@Setter
public class BlockchainProperties {
    
    private Contract contract = new Contract();
    private PrivateKeyConfig privateKeyConfig = new PrivateKeyConfig();
    private Api api = new Api();
    
    @Getter
    @Setter
    public static class Contract {
        @NotBlank(message = "Contract address cannot be blank")
        private String address;
    }
    
    @Getter
    @Setter
    public static class PrivateKeyConfig {
        @NotBlank(message = "Private key cannot be blank")
        private String key;
    }
    
    @Getter
    @Setter
    public static class Api {
        @NotBlank(message = "API URL cannot be blank")
        private String url;
    }
}
