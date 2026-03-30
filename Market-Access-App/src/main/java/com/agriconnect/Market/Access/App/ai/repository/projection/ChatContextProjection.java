package com.agriconnect.Market.Access.App.ai.repository.projection;

public interface ChatContextProjection {
    Long getSequenceNo();
    String getRole();
    String getContent();
}
