package com.agriconnect.api.gateway.Controller.Fallback;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Circuit-breaker fallback endpoints.
 *
 * When Resilience4j opens a circuit for a downstream service, Spring Cloud
 * Gateway MVC forwards the request to the matching /fallback/* endpoint
 * defined here, returning a 503 response instead of a timeout or error.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/contract-farming")
    public ResponseEntity<Map<String, Object>> contractFarmingFallback(HttpServletRequest request) {
        return buildFallback("Contract Farming", "Contracts, orders, and payment processing", request);
    }

    @RequestMapping("/market")
    public ResponseEntity<Map<String, Object>> marketAccessFallback(HttpServletRequest request) {
        return buildFallback("Market Access", "Listings and marketplace", request);
    }

    @RequestMapping("/agreement")
    public ResponseEntity<Map<String, Object>> generateAgreementFallback(HttpServletRequest request) {
        return buildFallback("Agreement Generator", "Cold storage and contract generation", request);
    }

    private ResponseEntity<Map<String, Object>> buildFallback(
            String serviceName,
            String serviceDescription,
            HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("service", serviceName);
        body.put("description", serviceDescription);
        body.put("message", "The '" + serviceName + "' service is temporarily unavailable. Please try again in a moment.");
        body.put("path", request.getRequestURI());
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
