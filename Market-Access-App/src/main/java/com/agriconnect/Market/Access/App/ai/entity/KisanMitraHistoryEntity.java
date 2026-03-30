package com.agriconnect.Market.Access.App.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_kisan_mitra_history",
        indexes = {
                @Index(name = "idx_ai_kisan_history_user_created", columnList = "user_phone,created_at"),
                @Index(name = "idx_ai_kisan_history_conversation_created", columnList = "conversation_id,created_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KisanMitraHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "conversation_id", nullable = false, length = 100)
    private String conversationId;

    @Column(name = "user_phone", nullable = false, length = 30)
    private String userPhone;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "user_message", nullable = false, length = 4000)
    private String userMessage;

    @Column(name = "assistant_response", nullable = false, length = 4000)
    private String assistantResponse;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "safety_decision", length = 40)
    private String safetyDecision;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
