package com.agriconnect.api.gateway.config;

import com.agriconnect.api.gateway.jwt.JwtAuthenticationFilter;
import com.agriconnect.api.gateway.filter.UserIdentityPropagationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class UserConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserIdentityPropagationFilter identityFilter;

    @Value("${cors.allowed-origins:http://localhost:5000,http://localhost:5174,http://localhost:1819}")
    private String allowedOrigins;

    @Autowired
    public UserConfig(JwtAuthenticationFilter jwtFilter, UserIdentityPropagationFilter identityFilter) {
        this.jwtFilter = jwtFilter;
        this.identityFilter = identityFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.authorizeHttpRequests(authorize -> {
            // Public: Swagger UI + OpenAPI docs (including proxied downstream docs)
            authorize.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/*/v3/api-docs",
                    "/*/v3/api-docs/**").permitAll();

            // Public: only health + prometheus for container checks and scraping
            authorize.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll();
            authorize.requestMatchers("/fallback/**", "/error").permitAll();

            // Public: Auth endpoints — register, login, and password reset
            authorize.requestMatchers(HttpMethod.POST, "/auth/register").permitAll();
            authorize.requestMatchers(HttpMethod.POST, "/auth/register/verify").permitAll();
            authorize.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
            authorize.requestMatchers(HttpMethod.POST, "/auth/forgot-password").permitAll();
            authorize.requestMatchers(HttpMethod.POST, "/auth/reset-password").permitAll();
            authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            authorize.requestMatchers(HttpMethod.GET, "/notifications/ws/**").permitAll();

            // Public: read-only user profile lookups (no sensitive data)
            authorize.requestMatchers(HttpMethod.GET, "/users/{phone}").permitAll();
            authorize.requestMatchers(HttpMethod.GET, "/users/unique/{id}").permitAll();
            authorize.requestMatchers(HttpMethod.GET, "/users/profile-image/{id}").permitAll();
            authorize.requestMatchers(HttpMethod.GET, "/users/signature-image/{id}").permitAll();
            authorize.requestMatchers(HttpMethod.GET, "/users/test").permitAll();

            // Everything else — including all gateway-proxied routes — requires a valid JWT
            authorize.anyRequest().authenticated();
        });

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // JWT filter populates SecurityContext; identity filter adds X-User-Phone to
        // proxied requests
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(identityFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration builder) throws Exception {
        return builder.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply gateway CORS only to HTTP API paths.
        // Do NOT apply it to `/notifications/ws/**`; the downstream SockJS endpoint
        // already sets CORS headers and duplicate headers break browsers.
        source.registerCorsConfiguration("/auth/**", configuration);
        source.registerCorsConfiguration("/users/**", configuration);
        source.registerCorsConfiguration("/market/**", configuration);
        source.registerCorsConfiguration("/contract-farming/**", configuration);
        source.registerCorsConfiguration("/agreement/**", configuration);
        source.registerCorsConfiguration("/notifications/api/**", configuration);
        source.registerCorsConfiguration("/notifications/v3/api-docs/**", configuration);
        // Include fallback/error paths so upstream failures still return CORS headers
        // (otherwise browser reports opaque CORS error instead of real 5xx payload).
        source.registerCorsConfiguration("/fallback/**", configuration);
        source.registerCorsConfiguration("/error", configuration);
        source.registerCorsConfiguration("/v3/api-docs/**", configuration);
        source.registerCorsConfiguration("/swagger-ui/**", configuration);
        source.registerCorsConfiguration("/swagger-ui.html", configuration);
        source.registerCorsConfiguration("/actuator/health", configuration);
        return source;
    }
}
