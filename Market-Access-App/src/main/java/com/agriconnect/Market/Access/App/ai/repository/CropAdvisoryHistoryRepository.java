package com.agriconnect.Market.Access.App.ai.repository;

import com.agriconnect.Market.Access.App.ai.entity.CropAdvisoryHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CropAdvisoryHistoryRepository extends JpaRepository<CropAdvisoryHistoryEntity, String> {
    Page<CropAdvisoryHistoryEntity> findByUserPhoneOrderByCreatedAtDesc(String userPhone, Pageable pageable);

    long deleteByUserPhone(String userPhone);

    @Modifying
    @Query(
            value = """
                    DELETE FROM ai_crop_advisory_history
                    WHERE id IN (
                        SELECT id
                        FROM ai_crop_advisory_history
                        WHERE created_at < :cutoff
                        ORDER BY created_at ASC
                        LIMIT :batchSize
                    )
                    """,
            nativeQuery = true
    )
    int deleteOldRows(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
