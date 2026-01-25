package com.agriconnect.Main.Backend.jwt;

import com.agriconnect.Main.Backend.Entity.BlackListedToken;
import com.agriconnect.Main.Backend.Repository.BlackListedTokenRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtAuthenticationHelper {

    private final BlackListedTokenRepo blackListedTokenRepo;
    
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final Long JWT_TOKEN_VALIDITY = Long.parseLong(String.valueOf(60*60*168));

    private final ObjectMapper objectMapper;

    public JwtAuthenticationHelper(BlackListedTokenRepo blackListedTokenRepo) {
        // Configure ObjectMapper to support Java 8 date/time
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.blackListedTokenRepo = blackListedTokenRepo;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getMobileNumberFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }

    public Boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        Date expDate = claims.getExpiration();
        return expDate.before(new Date());
    }

    public String generateToken(String phoneNumber) {
        Map<String, Object> claims = createClaims(phoneNumber);
        return Jwts.builder()
                .claims(claims)
                .subject(phoneNumber)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    private Map<String, Object> createClaims(String phoneNumber) {
        Map<String, Object> claims = new HashMap<>();
        System.out.println(phoneNumber + "==============");
        return claims;
    }

    private List<String> extractRoles(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("roles", List.class);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }

    public boolean isBlacklisted(String token) {
        Optional<BlackListedToken> blackListedToken = blackListedTokenRepo.findByToken(token);
        return blackListedToken.isPresent();
    }
}
