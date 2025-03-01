package com.agriconnect.Main.Backend.Repository;

import com.agriconnect.Main.Backend.Entity.BlackListedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlackListedTokenRepo extends JpaRepository<BlackListedToken, String> {
    Optional<BlackListedToken> findByToken(String token);
}