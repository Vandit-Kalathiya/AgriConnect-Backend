package com.agriconnect.notification.repository;

import com.agriconnect.notification.entity.Notification;
import com.agriconnect.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByEventIdAndChannel(String eventId, String channel);

    boolean existsByEventIdAndChannel(String eventId, String channel);

    long countByStatus(NotificationStatus status);

    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(String userId, String channel, Pageable pageable);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id")
    int markAsReadById(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.id = :id AND n.userId = :userId")
    int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);

    long countByStatusAndUserId(NotificationStatus status, String userId);
}
