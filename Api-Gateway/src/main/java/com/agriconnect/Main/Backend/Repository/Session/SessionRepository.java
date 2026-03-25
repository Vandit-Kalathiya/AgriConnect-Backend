package com.agriconnect.Main.Backend.Repository.Session;

import com.agriconnect.Main.Backend.Entity.Session.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    Optional<Session> findBySessionId(String sessionId);

    
}
