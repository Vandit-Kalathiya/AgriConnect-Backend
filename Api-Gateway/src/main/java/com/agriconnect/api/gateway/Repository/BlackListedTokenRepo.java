package com.agriconnect.api.gateway.Repository;

import com.agriconnect.api.gateway.Entity.BlackListedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlackListedTokenRepo extends JpaRepository<BlackListedToken, String> {
    Optional<BlackListedToken> findByToken(String token);
}