package com.agriconnect.Market.Access.App.ai.repository;

import com.agriconnect.Market.Access.App.ai.entity.AiConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, String> {
    Optional<AiConversationEntity> findByConversationId(String conversationId);
}
