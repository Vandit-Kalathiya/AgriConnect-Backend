package com.agriconnect.Market.Access.App.ai.filter;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiChatRateLimitFilter extends OncePerRequestFilter {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AiChatRateLimitFilter(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"/api/v1/ai/chat/respond".equals(request.getRequestURI())
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(0, Instant.now().plusSeconds(60)));

        boolean rateLimited;
        synchronized (bucket) {
            if (Instant.now().isAfter(bucket.resetAt)) {
                bucket.count = 0;
                bucket.resetAt = Instant.now().plusSeconds(60);
            }
            rateLimited = bucket.count >= aiProperties.getChatPublicPerMinute();
            if (!rateLimited) {
                bucket.count++;
            }
        }

        if (rateLimited) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Chatbot quota exceeded for this minute.")));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class Bucket {
        private int count;
        private Instant resetAt;

        private Bucket(int count, Instant resetAt) {
            this.count = count;
            this.resetAt = resetAt;
        }
    }
}
