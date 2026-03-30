package com.agriconnect.api.gateway.Repository.Token;

import com.agriconnect.api.gateway.Entity.Token.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TokenRepository extends JpaRepository<JwtToken, String> {

    JwtToken findByToken(String token);

    @Modifying
    @Query("""
            update JwtToken t
            set t.revoked = true, t.revokedAt = :now, t.expiresAt = :now
            where t.token = :token and t.revoked = false
            """)
    int revokeTokenByToken(@Param("token") String token, @Param("now") LocalDateTime now);
}
