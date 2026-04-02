package com.agriconnect.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP-over-WebSocket configuration.
 *
 * Connection flow:
 *   1. Client connects to  ws://host:2530/ws  (SockJS fallback: http://host:2530/ws)
 *   2. Client subscribes to  /topic/notifications/{userId}
 *   3. On every new IN_APP notification, Notification-Service publishes to that topic.
 *
 * Message destination convention:
 *   /topic/notifications/{userId}  — broadcast channel per user (server → client)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5000,http://localhost:5174,http://localhost:1819}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {        // In-memory broker for /topic destinations — no external broker needed
        registry.enableSimpleBroker("/topic");
        // Prefix for client-to-server messages (not used for push-only notifications)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                // SockJS fallback for browsers that don't support native WebSocket
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setMessageSizeLimit(128 * 1024)       // 128 KB max message
            .setSendBufferSizeLimit(512 * 1024)     // 512 KB send buffer
            .setSendTimeLimit(20_000);              // 20s send timeout
    }
}
