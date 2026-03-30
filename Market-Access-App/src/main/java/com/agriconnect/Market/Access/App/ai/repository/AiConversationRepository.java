package com.agriconnect.Market.Access.App.ai.repository;

import com.agriconnect.Market.Access.App.ai.entity.AiConversationEntity;
import com.agriconnect.Market.Access.App.ai.repository.projection.ChatConversationSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, String> {
    Optional<AiConversationEntity> findByConversationId(String conversationId);
    Optional<AiConversationEntity> findByConversationIdAndUserPhone(String conversationId, String userPhone);
    long deleteByConversationIdAndUserPhone(String conversationId, String userPhone);

    @Query(
            value = """
                    SELECT
                        c.conversation_id AS conversationId,
                        CASE
                            WHEN c.title IS NOT NULL AND c.title <> '' THEN c.title
                            WHEN lm.content IS NOT NULL AND lm.content <> '' THEN SUBSTRING(lm.content, 1, 40)
                            ELSE CONCAT('Chat ', c.conversation_id)
                        END AS title,
                        COALESCE(SUBSTRING(lm.content, 1, 120), '') AS lastMessagePreview,
                        c.created_at AS createdAt,
                        c.updated_at AS updatedAt
                    FROM ai_conversations c
                    LEFT JOIN ai_messages lm
                        ON lm.conversation_ref_id = c.id
                       AND lm.endpoint_type = 'CHAT'
                       AND lm.sequence_no = (
                           SELECT MAX(m2.sequence_no)
                           FROM ai_messages m2
                           WHERE m2.conversation_ref_id = c.id
                             AND m2.endpoint_type = 'CHAT'
                       )
                    WHERE c.user_phone = :userPhone
                    ORDER BY c.updated_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM ai_conversations c
                    WHERE c.user_phone = :userPhone
                    """,
            nativeQuery = true
    )
    Page<ChatConversationSummaryProjection> findConversationSummariesByUserPhone(
            @Param("userPhone") String userPhone,
            Pageable pageable
    );

    @Modifying
    @Query(
            value = """
                    DELETE FROM ai_conversations c
                    WHERE c.id IN (
                        SELECT c2.id
                        FROM ai_conversations c2
                        WHERE c2.updated_at < :cutoff
                          AND NOT EXISTS (
                              SELECT 1
                              FROM ai_messages m
                              WHERE m.conversation_ref_id = c2.id
                                AND m.endpoint_type = 'CHAT'
                          )
                        ORDER BY c2.updated_at ASC
                        LIMIT :batchSize
                    )
                    """,
            nativeQuery = true
    )
    int deleteOldOrphanConversations(@Param("cutoff") java.time.LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
