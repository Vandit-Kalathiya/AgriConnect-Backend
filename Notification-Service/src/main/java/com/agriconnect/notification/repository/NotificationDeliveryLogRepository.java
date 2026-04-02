package com.agriconnect.notification.repository;

import com.agriconnect.notification.entity.NotificationDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, UUID> {

    boolean existsByEventIdAndChannel(String eventId, String channel);
}
