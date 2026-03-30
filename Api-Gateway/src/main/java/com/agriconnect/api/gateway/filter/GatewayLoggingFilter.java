package com.agriconnect.api.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every inbound request and its response status/latency.
 * Runs at the highest priority so it wraps the full processing time,
 * including JWT validation, Spring Security checks, and gateway proxying.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri    = request.getRequestURI();
        String query  = request.getQueryString();
        long   start  = System.currentTimeMillis();

        log.info("→ {} {}{}", method, uri, query != null ? "?" + query : "");

        try {
            chain.doFilter(request, response);
        } finally {
            long ms = System.currentTimeMillis() - start;
            log.info("← {} {} | status={} | {}ms", method, uri, response.getStatus(), ms);
        }
    }
}
