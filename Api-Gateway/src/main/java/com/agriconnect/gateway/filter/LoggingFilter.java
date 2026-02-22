package com.agriconnect.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that logs every request and response passing through the gateway.
 * Runs at highest priority (order = -1) to capture full timing.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path   = request.getURI().getPath();
        String query  = request.getURI().getQuery();
        long startTime = System.currentTimeMillis();

        log.info("→ {} {}{}",
                method,
                path,
                query != null ? "?" + query : "");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long durationMs = System.currentTimeMillis() - startTime;
            log.info("← {} {} | status={} | {}ms",
                    method,
                    path,
                    exchange.getResponse().getStatusCode(),
                    durationMs);
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
