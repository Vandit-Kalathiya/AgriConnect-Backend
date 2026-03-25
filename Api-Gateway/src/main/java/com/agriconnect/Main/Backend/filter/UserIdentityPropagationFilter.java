package com.agriconnect.Main.Backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Runs immediately after the JWT filter in the Spring Security chain.
 *
 * When the incoming request carries a valid JWT, this filter wraps the
 * HttpServletRequest to add an {@code X-User-Phone} header whose value is
 * the authenticated user's phone number.  Spring Cloud Gateway MVC copies
 * all request headers when proxying to downstream services, so every
 * downstream service (Market-Access-App, Contract-Farming-App, etc.) can
 * read this header to identify the caller without needing their own JWT
 * validation logic.
 *
 * Downstream services should treat this header as authoritative because
 * all external traffic must pass through this gateway, which has already
 * verified the JWT before the request reaches them.
 */
@Component
public class UserIdentityPropagationFilter extends OncePerRequestFilter {

    public static final String USER_PHONE_HEADER = "X-User-Phone";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String phoneNumber = auth.getName();
            chain.doFilter(new PhoneHeaderRequestWrapper(request, phoneNumber), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Decorates the original HttpServletRequest to inject the X-User-Phone header.
     * Spring Cloud Gateway MVC reads all headers from the HttpServletRequest when
     * building the upstream proxy call, so this wrapper ensures the header is present.
     */
    private static class PhoneHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final String phoneNumber;

        PhoneHeaderRequestWrapper(HttpServletRequest request, String phoneNumber) {
            super(request);
            this.phoneNumber = phoneNumber;
        }

        @Override
        public String getHeader(String name) {
            if (USER_PHONE_HEADER.equalsIgnoreCase(name)) {
                return phoneNumber;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (USER_PHONE_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singleton(phoneNumber));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            if (!names.stream().anyMatch(USER_PHONE_HEADER::equalsIgnoreCase)) {
                names.add(USER_PHONE_HEADER);
            }
            return Collections.enumeration(names);
        }
    }
}
