package com.agriconnect.notification.controller;

import com.agriconnect.notification.entity.Notification;
import com.agriconnect.notification.entity.NotificationStatus;
import com.agriconnect.notification.repository.NotificationDeliveryLogRepository;
import com.agriconnect.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;

    /**
     * GET /api/notifications?userId=&channel=&page=0&size=20
     * Returns paginated IN_APP notifications for a user.
     * If channel is not provided, returns all channels.
     */
    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestParam String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> result = (channel != null && !channel.isBlank())
                ? notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(userId, channel, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/notifications/unread-count?userId=
     * Returns number of unread IN_APP notifications for badge display.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam String userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/notifications/{id}/read
     * Marks a single notification as read.
     */
    @PatchMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable UUID id) {
        int updated = notificationRepository.markAsReadById(id);
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("read", true);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/notifications/read-all?userId=
     * Marks all notifications as read for a user.
     */
    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestParam String userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("updated", updated);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/notifications/{id}?userId=
     * Deletes a notification — userId is required to prevent cross-user deletion.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @RequestParam String userId) {

        int deleted = notificationRepository.deleteByIdAndUserId(id, userId);
        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/notifications/stats
     * Returns aggregate counts by status and channel for admin dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSent",    notificationRepository.countByStatus(NotificationStatus.SENT));
        stats.put("totalFailed",  notificationRepository.countByStatus(NotificationStatus.FAILED));
        stats.put("totalPending", notificationRepository.countByStatus(NotificationStatus.PENDING));
        stats.put("totalSkipped", notificationRepository.countByStatus(NotificationStatus.SKIPPED_DUPLICATE));
        stats.put("total",        notificationRepository.count());

        long totalDeliveryLogs = deliveryLogRepository.count();
        stats.put("totalDeliveryLogs", totalDeliveryLogs);

        return ResponseEntity.ok(stats);
    }
}
