package com.agriconnect.Market.Access.App.ai.repository.projection;

import java.time.LocalDateTime;

public interface ChatConversationSummaryProjection {
    String getConversationId();
    String getTitle();
    String getLastMessagePreview();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
