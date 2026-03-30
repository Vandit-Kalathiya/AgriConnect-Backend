package com.agriconnect.api.gateway.Service.Auth;

import com.agriconnect.api.gateway.Repository.Session.SessionRepository;
import com.agriconnect.api.gateway.Service.BlackListedTokenService;
import com.agriconnect.api.gateway.Service.Token.TokenService;
import com.agriconnect.api.gateway.jwt.JwtAuthenticationHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final JwtAuthenticationHelper jwtHelper;
    private final BlackListedTokenService blackListedTokenService;
    private final TokenService tokenService;
    private final SessionRepository sessionRepository;

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            clearJwtCookie(response);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Date expiryDate = new Date();
        String username = null;

        try {
            expiryDate = jwtHelper.getExpirationDateFromToken(token);
            username = jwtHelper.getMobileNumberFromToken(token);
        } catch (Exception ignored) {
            // Continue with forced logout flow even for malformed/expired tokens
        }

        // 1) Blacklist token first
        blackListedTokenService.blacklistToken(token, expiryDate);

        // 2) Mark token/session as expired in persistence
        tokenService.revokeToken(token, now);
        if (username != null && !username.isBlank()) {
            sessionRepository.expireActiveSessionsByUsername(username, now);
        }

        // 3) Finally clear auth cookie from client
        clearJwtCookie(response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("jwt_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
