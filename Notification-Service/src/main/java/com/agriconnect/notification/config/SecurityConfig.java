package com.agriconnect.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Notification-Service.
 *
 * This service is internal — all traffic arrives via the API Gateway.
 * CORS is intentionally disabled here: the gateway owns and sets the
 * Access-Control-Allow-Origin header. If both services set it, the header
 * is duplicated and browsers reject the request.
 *
 * WebSocket origin filtering is handled separately in WebSocketConfig
 * via setAllowedOriginPatterns().
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)   // Gateway handles CORS — do NOT re-add here
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**", "/ws/info/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
