package com.agriconnect.api.gateway.Service.Token;

import com.agriconnect.api.gateway.Entity.Token.JwtToken;
import com.agriconnect.api.gateway.Repository.Token.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

//    @CachePut(value = "tokens", key = "#token.id")
    public JwtToken saveToken(JwtToken token) {
        return tokenRepository.save(token);
    }

//    @Cacheable(value = "tokens", key = "#token")
    public JwtToken getToken(String token) {
        return tokenRepository.findByToken(token);
    }

//    @CacheEvict(value = "tokens", key = "#token")
    public void deleteToken(String token) {
        JwtToken jwtToken = tokenRepository.findByToken(token);
        if (jwtToken != null) {
            tokenRepository.deleteById(jwtToken.getId());
        }
    }

    @Transactional
    public int revokeToken(String token, LocalDateTime now) {
        return tokenRepository.revokeTokenByToken(token, now);
    }
}
