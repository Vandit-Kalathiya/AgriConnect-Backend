package com.agriconnect.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AgriConnect API Gateway",
                description = "Unified entry point for all AgriConnect backend microservices. " +
                        "Use the service selector in the top-right corner to switch between services.",
                version = "1.0.0",
                contact = @Contact(
                        name = "AgriConnect Team"
                )
        )
)
public class OpenApiConfig {
}
