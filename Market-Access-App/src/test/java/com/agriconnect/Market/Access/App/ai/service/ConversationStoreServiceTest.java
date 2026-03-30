package com.agriconnect.Market.Access.App.ai.service;

import com.agriconnect.Market.Access.App.ai.config.AiProperties;
import com.agriconnect.Market.Access.App.ai.dto.ChatDtos;
import com.agriconnect.Market.Access.App.ai.entity.AiConversationEntity;
import com.agriconnect.Market.Access.App.ai.entity.AiMessageEntity;
import com.agriconnect.Market.Access.App.ai.repository.AiConversationRepository;
import com.agriconnect.Market.Access.App.ai.repository.AiMessageRepository;
import com.agriconnect.Market.Access.App.ai.repository.projection.ChatContextProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationStoreServiceTest {

    @Mock
    private AiConversationRepository conversationRepository;

    @Mock
    private AiMessageRepository messageRepository;

    @Mock
    private SafetyPolicyService safetyPolicyService;

    private ConversationStoreService service;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.getPersistence().setEnabled(true);
        properties.getPersistence().setMaxContentLength(3900);
        service = new ConversationStoreService(conversationRepository, messageRepository, safetyPolicyService, properties);
    }

    @Test
    void ensureConversation_createsWhenNotFound() {
        when(conversationRepository.findByConversationId("cid-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(AiConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiConversationEntity conversation = service.ensureConversation("cid-1", "9999999999", "en");

        assertThat(conversation.getConversationId()).isEqualTo("cid-1");
        assertThat(conversation.getUserPhone()).isEqualTo("9999999999");
        verify(conversationRepository).save(any(AiConversationEntity.class));
    }

    @Test
    void appendIncomingMessages_savesBatchWithIncrementingSequence() {
        AiConversationEntity conversation = AiConversationEntity.builder().conversationId("cid-2").build();
        List<ChatDtos.ChatMessage> messages = List.of(
                ChatDtos.ChatMessage.builder().role("user").content("first").build(),
                ChatDtos.ChatMessage.builder().role("user").content("second").build()
        );
        when(messageRepository.findMaxSequenceNoByConversationId("cid-2")).thenReturn(5L);
        when(safetyPolicyService.redactPii(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.appendIncomingMessages(conversation, messages);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiMessageEntity>> captor = (ArgumentCaptor<List<AiMessageEntity>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(messageRepository).saveAll(captor.capture());
        List<AiMessageEntity> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getSequenceNo()).isEqualTo(6L);
        assertThat(saved.get(1).getSequenceNo()).isEqualTo(7L);
    }

    @Test
    void loadRecentChatContext_returnsAscendingOrder() {
        ChatContextProjection newer = projection(11L, "assistant", "B");
        ChatContextProjection older = projection(10L, "user", "A");
        when(messageRepository.findRecentChatContextByConversationId(eq("cid-3"), any(Pageable.class)))
                .thenReturn(List.of(newer, older));

        List<ChatDtos.ChatMessage> context = service.loadRecentChatContext("cid-3", 10);

        assertThat(context).hasSize(2);
        assertThat(context.get(0).getContent()).isEqualTo("A");
        assertThat(context.get(1).getContent()).isEqualTo("B");
    }

    private ChatContextProjection projection(long seq, String role, String content) {
        return new ChatContextProjection() {
            @Override
            public Long getSequenceNo() {
                return seq;
            }

            @Override
            public String getRole() {
                return role;
            }

            @Override
            public String getContent() {
                return content;
            }
        };
    }
}
