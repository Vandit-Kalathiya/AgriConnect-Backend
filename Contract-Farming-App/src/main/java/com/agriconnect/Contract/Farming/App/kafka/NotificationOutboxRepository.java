package com.agriconnect.Contract.Farming.App.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEntry, UUID> {

    List<NotificationOutboxEntry> findTop50ByStatusOrderByCreatedAtAsc(
            NotificationOutboxEntry.OutboxStatus status);
}
