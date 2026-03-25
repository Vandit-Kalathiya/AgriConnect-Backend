package com.agriconnect.Main.Backend.config;

import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> contractFarmingRoute() {
        return GatewayRouterFunctions.route("contract-farming-cb")
                .route(path("/contract-farming/**"), HandlerFunctions.http("lb://Contract-Farming-App"))
                .before(BeforeFilterFunctions.stripPrefix(1))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker("contractFarming",
                        URI.create("forward:/fallback/contract-farming")))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> marketAccessRoute() {
        return GatewayRouterFunctions.route("market-access-cb")
                .route(path("/market/**"), HandlerFunctions.http("lb://Market-Access-App"))
                .before(BeforeFilterFunctions.stripPrefix(1))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker("marketAccess",
                        URI.create("forward:/fallback/market")))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> generateAgreementRoute() {
        return GatewayRouterFunctions.route("generate-agreement-cb")
                .route(path("/agreement/**"), HandlerFunctions.http("lb://Generate-Agreement-App"))
                .before(BeforeFilterFunctions.stripPrefix(1))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker("generateAgreement",
                        URI.create("forward:/fallback/agreement")))
                .build();
    }
}
