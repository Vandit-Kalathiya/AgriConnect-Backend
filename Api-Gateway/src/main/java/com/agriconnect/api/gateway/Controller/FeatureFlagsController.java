package com.agriconnect.api.gateway.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feature-flags")
public class FeatureFlagsController {

    @Value("${feature.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${feature.mobile-verification.enabled:true}")
    private boolean mobileVerificationEnabled;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getFeatureFlags() {
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("kafka", kafkaEnabled);
        flags.put("redis", redisEnabled);
        flags.put("mobileVerification", mobileVerificationEnabled);
        return ResponseEntity.ok(flags);
    }
}
