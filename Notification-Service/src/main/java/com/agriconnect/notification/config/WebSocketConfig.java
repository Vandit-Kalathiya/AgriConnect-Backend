package com.agriconnect.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP-over-WebSocket configuration optimized for high-concurrency real-time streaming.
 *
 * Connection flow:
 *   1. Client connects to  ws://host:2530/ws  (SockJS fallback: http://host:2530/ws)
 *   2. Client subscribes to  /topic/notifications/{userId}
 *   3. On every new IN_APP notification, Notification-Service publishes to that topic.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5000,http://localhost:5174,http://localhost:1819}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Scheduler for sending pings/heartbeats to keep client connections alive through gateways/proxies
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-thread-");
        heartbeatScheduler.initialize();

        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000}) // Ping every 10s, expect pong every 10s
                .setTaskScheduler(heartbeatScheduler);

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS(); // SockJS fallback
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setMessageSizeLimit(128 * 1024)       // 128 KB max message size
            .setSendBufferSizeLimit(512 * 1024)     // 512 KB send buffer limit
            .setSendTimeLimit(20_000);              // 20s send timeout limit
    }
}
