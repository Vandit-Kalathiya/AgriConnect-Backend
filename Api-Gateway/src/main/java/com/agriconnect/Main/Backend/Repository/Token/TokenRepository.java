package com.agriconnect.Main.Backend.Repository.Token;

import com.agriconnect.Main.Backend.Entity.Token.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<JwtToken, String> {

    JwtToken findByToken(String token);
}
