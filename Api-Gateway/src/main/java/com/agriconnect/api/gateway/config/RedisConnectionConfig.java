package com.agriconnect.api.gateway.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConnectionConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);

        // Set password if provided (AUTH token for AWS ElastiCache Valkey)
        if (StringUtils.hasText(password)) {
            logger.info("Configuring Redis/Valkey with password authentication");
            redisConfig.setPassword(password);
        } else {
            logger.info("Configuring Redis/Valkey without password (local development)");
        }

        // Configure Lettuce client
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettucePoolingClientConfiguration
                .builder()
                .commandTimeout(Duration.ofMillis(2000));

        // Enable SSL for production
        if (sslEnabled) {
            logger.info("Enabling SSL for Redis connection");
            clientConfig.useSsl();
        }

        // Socket options
        clientConfig.clientOptions(
                ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(2000))
                                .build())
                        .build());

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig.build());

        logger.info("Redis/Valkey connection factory configured for {}:{} (SSL: {})",
                host, port, sslEnabled);

        return factory;
    }
}
