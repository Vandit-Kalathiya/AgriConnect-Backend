package com.agriconnect.api.gateway.Repository.Session;

import com.agriconnect.api.gateway.Entity.Session.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    Optional<Session> findBySessionId(String sessionId);

    @Modifying
    @Query("""
            update Session s
            set s.expiresAt = :now
            where s.username = :username and s.expiresAt > :now
            """)
    int expireActiveSessionsByUsername(@Param("username") String username, @Param("now") LocalDateTime now);
}
