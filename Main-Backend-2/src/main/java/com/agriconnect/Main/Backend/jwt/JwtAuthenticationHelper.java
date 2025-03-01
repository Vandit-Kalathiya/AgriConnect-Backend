package com.agriconnect.Main.Backend.jwt;

import com.agriconnect.Main.Backend.Entity.BlackListedToken;
import com.agriconnect.Main.Backend.Repository.BlackListedTokenRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
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

    public String getMobileNumberFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(SECRET_KEY.getBytes())
                .build().parseClaimsJws(token).getBody();
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
                .setClaims(claims)
                .setSubject(phoneNumber)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(
                        new SecretKeySpec(SECRET_KEY.getBytes(), SignatureAlgorithm.HS512.getJcaName()),
                        SignatureAlgorithm.HS512
                )
                .serializeToJsonWith(claimsMap -> {
                    try {
                        return objectMapper.writeValueAsString(claimsMap).getBytes();
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException("Error serializing claims to JSON", e);
                    }
                })
                .compact();
    }

    private Map<String, Object> createClaims(String phoneNumber) {
        Map<String, Object> claims = new HashMap<>();
        System.out.println(phoneNumber + "==============");
        return claims;
    }

    private SecretKeySpec getSigningKey() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), SignatureAlgorithm.HS512.getJcaName());
    }

    private List<String> extractRoles(String token) {
        Claims claims = Jwts.parser().setSigningKey(getSigningKey()).parseClaimsJws(token).getBody();
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
