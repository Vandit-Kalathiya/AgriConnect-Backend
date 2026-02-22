package com.agriconnect.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles circuit-breaker fallback responses for all downstream services.
 * Each route in application.yml forwards to /fallback/{service} when the
 * circuit breaker opens or the downstream service times out.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/main")
    public Mono<ResponseEntity<Map<String, Object>>> mainBackendFallback(ServerWebExchange exchange) {
        return buildFallback("Main Backend", "Authentication and user management", exchange);
    }

    @RequestMapping("/contract-farming")
    public Mono<ResponseEntity<Map<String, Object>>> contractFarmingFallback(ServerWebExchange exchange) {
        return buildFallback("Contract Farming", "Contracts, orders, and payment processing", exchange);
    }

    @RequestMapping("/market")
    public Mono<ResponseEntity<Map<String, Object>>> marketAccessFallback(ServerWebExchange exchange) {
        return buildFallback("Market Access", "Listings and marketplace", exchange);
    }

    @RequestMapping("/agreement")
    public Mono<ResponseEntity<Map<String, Object>>> generateAgreementFallback(ServerWebExchange exchange) {
        return buildFallback("Agreement Generator", "Cold storage and contract generation", exchange);
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallback(
            String serviceName,
            String serviceDescription,
            ServerWebExchange exchange) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("service", serviceName);
        body.put("description", serviceDescription);
        body.put("message", "The '" + serviceName + "' service is temporarily unavailable. Please try again in a moment.");
        body.put("path", exchange.getRequest().getURI().getPath());
        body.put("timestamp", LocalDateTime.now().toString());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
