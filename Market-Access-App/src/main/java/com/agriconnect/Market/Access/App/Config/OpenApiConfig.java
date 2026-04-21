package com.agriconnect.Market.Access.App.Config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Provide the JWT token obtained from /auth/login. Format: Bearer <token>"
)
public class OpenApiConfig {

    @Value("${GATEWAY_URL:http://localhost:8080}")
    private String gatewayUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Market Access Service")
                        .description("Manages product listings for the AgriConnect marketplace, including creation, updates, status management, and image retrieval.")
                        .version("1.0.0")
                        .contact(new Contact().name("AgriConnect Team")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server()
                                .url(gatewayUrl + "/market")
                                .description("API Gateway")
                ));
    }

    @Configuration
    @EnableCaching
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = true)
    public static class NoOpCacheConfig {

        private static final Logger logger = LoggerFactory.getLogger(NoOpCacheConfig.class);

        @Bean
        public CacheManager cacheManager() {
            logger.warn("Redis/Valkey caching is DISABLED. Using NoOpCacheManager. All @Cacheable annotations will be no-ops.");
            logger.warn("To enable caching, set redis.enabled=true in application properties");
            return new NoOpCacheManager();
        }
    }

    @Configuration
    @EnableCaching
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
    public static class RedisConfig {

        private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            logger.info("Initializing RedisTemplate with Valkey-compatible configuration");

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

            template.setKeySerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);

            template.afterPropertiesSet();

            logger.info("RedisTemplate initialized successfully");
            return template;
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
            logger.info("Configuring RedisCacheManager with per-cache TTL settings");

            GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                    .disableCachingNullValues();

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            cacheConfigurations.put("listingCache", defaultConfig.entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("productCache", defaultConfig.entryTtl(Duration.ofHours(6)));
            cacheConfigurations.put("categoryCache", defaultConfig.entryTtl(Duration.ofDays(1)));
            cacheConfigurations.put("searchCache", defaultConfig.entryTtl(Duration.ofMinutes(30)));
            cacheConfigurations.put("imageCache", defaultConfig.entryTtl(Duration.ofDays(7)));

            RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .transactionAware()
                    .build();

            logger.info("RedisCacheManager configured with {} custom cache configurations", cacheConfigurations.size());
            return cacheManager;
        }

        private GenericJackson2JsonRedisSerializer createJsonSerializer() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            objectMapper.activateDefaultTyping(
                    BasicPolymorphicTypeValidator.builder()
                            .allowIfBaseType(Object.class)
                            .build(),
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
            );

            return new GenericJackson2JsonRedisSerializer(objectMapper);
        }
    }
}
