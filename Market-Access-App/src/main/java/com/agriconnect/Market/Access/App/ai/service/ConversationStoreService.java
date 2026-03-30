package com.agriconnect.Market.Access.App.ai.service;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.dto.AiEnums;
import com.agriconnect.Market.Access.App.ai.dto.ChatDtos;
import com.agriconnect.Market.Access.App.ai.entity.AiConversationEntity;
import com.agriconnect.Market.Access.App.ai.entity.AiMessageEntity;
import com.agriconnect.Market.Access.App.ai.repository.AiConversationRepository;
import com.agriconnect.Market.Access.App.ai.repository.AiMessageRepository;
import com.agriconnect.Market.Access.App.ai.repository.projection.ChatContextProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationStoreService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final SafetyPolicyService safetyPolicyService;
    private final AiProperties aiProperties;

    @Transactional
    public AiConversationEntity ensureConversation(String conversationId, String userPhone, String language) {
        if (!aiProperties.getPersistence().isEnabled()) {
            return AiConversationEntity.builder()
                    .conversationId(ensureConversationId(conversationId))
                    .userPhone(userPhone)
                    .language(language)
                    .status("ACTIVE")
                    .build();
        }
        String resolvedConversationId = ensureConversationId(conversationId);
        AiConversationEntity conversation = conversationRepository.findByConversationId(resolvedConversationId)
                .orElseGet(() -> conversationRepository.save(AiConversationEntity.builder()
                        .conversationId(resolvedConversationId)
                        .userPhone(userPhone)
                        .language(language)
                        .status("ACTIVE")
                        .build()));
        if (userPhone != null && !userPhone.isBlank() && !userPhone.equals(conversation.getUserPhone())) {
            conversation.setUserPhone(userPhone);
            conversationRepository.save(conversation);
        }
        return conversation;
    }

    public String ensureConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    @Transactional
    public long appendIncomingMessages(AiConversationEntity conversation, List<ChatDtos.ChatMessage> incoming) {
        if (!aiProperties.getPersistence().isEnabled()) {
            return 0;
        }
        if (incoming == null || incoming.isEmpty()) {
            return messageRepository.findMaxSequenceNoByConversationId(conversation.getConversationId());
        }
        long currentMax = messageRepository.findMaxSequenceNoByConversationId(conversation.getConversationId());
        long next = currentMax + 1;
        List<AiMessageEntity> rows = new java.util.ArrayList<>(incoming.size());
        for (ChatDtos.ChatMessage msg : incoming) {
            rows.add(AiMessageEntity.builder()
                    .conversation(conversation)
                    .sequenceNo(next++)
                    .role(msg.getRole())
                    .endpointType("CHAT")
                    .content(truncate(safetyPolicyService.redactPii(msg.getContent())))
                    .build());
        }
        messageRepository.saveAll(rows);
        return currentMax + rows.size();
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ChatMessage> loadRecentChatContext(String conversationId, int limit) {
        if (!aiProperties.getPersistence().isEnabled()) {
            return Collections.emptyList();
        }
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<ChatContextProjection> rows = messageRepository.findRecentChatContextByConversationId(
                conversationId,
                PageRequest.of(0, limit)
        );
        rows.sort(Comparator.comparingLong(ChatContextProjection::getSequenceNo));
        return rows.stream()
                .map(row -> ChatDtos.ChatMessage.builder()
                        .role(row.getRole())
                        .content(row.getContent() == null ? "" : row.getContent())
                        .build())
                .toList();
    }

    @Transactional
    public void appendAssistantResponse(
            AiConversationEntity conversation,
            String content,
            AiEnums.ResponseSource source,
            AiEnums.SafetyDecision safetyDecision
    ) {
        if (!aiProperties.getPersistence().isEnabled()) {
            return;
        }
        long currentMax = messageRepository.findMaxSequenceNoByConversationId(conversation.getConversationId());
        messageRepository.save(AiMessageEntity.builder()
                .conversation(conversation)
                .sequenceNo(currentMax + 1)
                .role("assistant")
                .endpointType("CHAT")
                .content(truncate(safetyPolicyService.redactPii(content)))
                .source(source == null ? null : source.name())
                .safetyDecision(safetyDecision == null ? null : safetyDecision.name())
                .build());
    }

    @Transactional
    public void appendEndpointInteraction(
            String endpointType,
            String userPhone,
            String language,
            String requestPayload,
            String responsePayload,
            AiEnums.ResponseSource source,
            AiEnums.SafetyDecision safetyDecision
    ) {
        if (!aiProperties.getPersistence().isEnabled()) {
            return;
        }
        AiConversationEntity conversation = ensureConversation(null, userPhone, language);
        long currentMax = messageRepository.findMaxSequenceNoByConversationId(conversation.getConversationId());
        messageRepository.save(AiMessageEntity.builder()
                .conversation(conversation)
                .sequenceNo(currentMax + 1)
                .role("system")
                .endpointType(endpointType)
                .requestPayload(truncate(safetyPolicyService.redactPii(requestPayload)))
                .responsePayload(truncate(safetyPolicyService.redactPii(responsePayload)))
                .source(source == null ? null : source.name())
                .safetyDecision(safetyDecision == null ? null : safetyDecision.name())
                .build());
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        int maxLength = aiProperties.getPersistence().getMaxContentLength();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
