package com.agriconnect.Market.Access.App.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_messages",
        indexes = {
                @Index(name = "idx_ai_msg_conversation_sequence", columnList = "conversation_ref_id,sequence_no"),
                @Index(name = "idx_ai_msg_conversation_created", columnList = "conversation_ref_id,created_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_ref_id", nullable = false)
    private AiConversationEntity conversation;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "endpoint_type", nullable = false, length = 50)
    private String endpointType;

    @Column(name = "content", length = 4000)
    private String content;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "safety_decision", length = 40)
    private String safetyDecision;

    @Column(name = "request_payload", length = 4000)
    private String requestPayload;

    @Column(name = "response_payload", length = 4000)
    private String responsePayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
