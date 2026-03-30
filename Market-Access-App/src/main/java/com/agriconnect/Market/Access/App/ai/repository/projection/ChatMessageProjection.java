package com.agriconnect.Market.Access.App.ai.repository.projection;

import java.time.LocalDateTime;

public interface ChatMessageProjection {
    Long getSequenceNo();
    String getRole();
    String getContent();
    String getSource();
    String getSafetyDecision();
    LocalDateTime getCreatedAt();
}
