package com.agriconnect.Market.Access.App.ai.repository;

import com.agriconnect.Market.Access.App.ai.entity.AiMessageEntity;
import com.agriconnect.Market.Access.App.ai.repository.projection.ChatContextProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessageEntity, String> {

    @Query("""
            select coalesce(max(m.sequenceNo), 0)
            from AiMessageEntity m
            where m.conversation.conversationId = :conversationId
            """)
    long findMaxSequenceNoByConversationId(@Param("conversationId") String conversationId);

    @Query("""
            select m.sequenceNo as sequenceNo, m.role as role, m.content as content
            from AiMessageEntity m
            where m.conversation.conversationId = :conversationId
              and m.endpointType = 'CHAT'
            order by m.sequenceNo desc
            """)
    List<ChatContextProjection> findRecentChatContextByConversationId(
            @Param("conversationId") String conversationId,
            Pageable pageable
    );

    @Query("""
            select m.id
            from AiMessageEntity m
            where m.createdAt < :cutoffTime
            order by m.createdAt asc
            """)
    List<String> findIdsForCleanup(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable
    );

    long deleteByIdIn(List<String> ids);
}
