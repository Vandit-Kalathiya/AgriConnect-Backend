package com.agriconnect.api.gateway.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT obtained from POST /auth/verify-otp or cookie jwt_token. Format: Bearer <token>"
)
public class OpenApiConfig {

    @Value("${GATEWAY_URL:http://localhost:8080}")
    private String gatewayUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgriConnect — API Gateway")
                        .description("""
                                Central entry point for the AgriConnect platform.

                                **Authentication flow:**
                                1. `POST /auth/register` → receive OTP via SMS
                                2. `POST /auth/r/verify-otp/{phone}/{otp}` → create account
                                3. `POST /auth/login` → receive login OTP
                                4. `POST /auth/verify-otp/{phone}/{otp}` → receive JWT (cookie + JSON)
                                5. Include the JWT in every subsequent request via cookie `jwt_token` \\
                                   or `Authorization: Bearer <token>` header.

                                **Proxied services (require JWT):**
                                - `/market/**` → Market Access App (Listings)
                                - `/contract-farming/**` → Contract Farming App (Agreements, Orders, Payments)
                                - `/agreement/**` → Agreement Generator (PDF contracts, Cold Storage)
                                """)
                        .version("2.0.0")
                        .contact(new Contact().name("AgriConnect Team")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server()
                                .url(gatewayUrl)
                                .description("API Gateway")
                ));
    }
}
