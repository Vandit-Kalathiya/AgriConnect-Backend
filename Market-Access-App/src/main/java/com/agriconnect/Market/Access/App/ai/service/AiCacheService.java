package com.agriconnect.Market.Access.App.ai.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiCacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Optional<String> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public void put(String key, String value, int ttlSeconds) {
        cache.put(key, new CacheEntry(value, Instant.now().plusSeconds(ttlSeconds)));
    }

    private record CacheEntry(String value, Instant expiresAt) {
    }
}
