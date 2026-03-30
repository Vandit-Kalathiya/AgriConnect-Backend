package com.agriconnect.Market.Access.App.ai.repository;

import com.agriconnect.Market.Access.App.ai.entity.KisanMitraHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface KisanMitraHistoryRepository extends JpaRepository<KisanMitraHistoryEntity, String> {
    Page<KisanMitraHistoryEntity> findByUserPhoneOrderByCreatedAtDesc(String userPhone, Pageable pageable);

    long deleteByUserPhone(String userPhone);

    long deleteByUserPhoneAndConversationId(String userPhone, String conversationId);

    @Modifying
    @Query(
            value = """
                    DELETE FROM ai_kisan_mitra_history
                    WHERE id IN (
                        SELECT id
                        FROM ai_kisan_mitra_history
                        WHERE created_at < :cutoff
                        ORDER BY created_at ASC
                        LIMIT :batchSize
                    )
                    """,
            nativeQuery = true
    )
    int deleteOldRows(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
