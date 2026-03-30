package com.agriconnect.Market.Access.App.Config;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * All authentication and authorization is handled centrally by the API Gateway.
 * CORS is not configured here — it is irrelevant for server-to-server calls from the gateway.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class Config {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
