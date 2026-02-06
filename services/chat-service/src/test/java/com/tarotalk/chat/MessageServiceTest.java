package com.tarotalk.chat;

import com.tarotalk.chat.api.MessageContext;
import com.tarotalk.chat.api.MessageSendRequest;
import com.tarotalk.chat.domain.ChatMessage;
import com.tarotalk.chat.domain.Conversation;
import com.tarotalk.chat.domain.ConversationParticipant;
import com.tarotalk.chat.repo.ChatMessageRepository;
import com.tarotalk.chat.repo.ConversationParticipantRepository;
import com.tarotalk.chat.repo.ConversationRepository;
import com.tarotalk.chat.service.AiClient;
import com.tarotalk.chat.service.MessageService;
import com.tarotalk.chat.service.OrchestratorClient;
import com.tarotalk.chat.websocket.WebSocketPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MessageServiceTest {
    @Test
    void buildContextWhenMissing() {
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ConversationParticipantRepository participantRepository = mock(ConversationParticipantRepository.class);
        AiClient aiClient = mock(AiClient.class);
        OrchestratorClient orchestratorClient = mock(OrchestratorClient.class);
        WebSocketPublisher webSocketPublisher = mock(WebSocketPublisher.class);

        MessageService messageService = new MessageService(
                chatMessageRepository,
                conversationRepository,
                participantRepository,
                aiClient,
                orchestratorClient,
                webSocketPublisher
        );

        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Conversation conversation = new Conversation(conversationId, Conversation.Type.ONE_ON_ONE, "test");
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        ChatMessage recent = new ChatMessage();
        recent.setConversationId(conversationId);
        recent.setSenderId(otherId);
        recent.setContent("second");
        recent.setSentAt(Instant.now());

        ChatMessage earlier = new ChatMessage();
        earlier.setConversationId(conversationId);
        earlier.setSenderId(senderId);
        earlier.setContent("first");
        earlier.setSentAt(Instant.now().minusSeconds(60));

        when(chatMessageRepository.findByConversationIdOrderBySentAtDesc(eq(conversationId), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(recent, earlier)));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(participantRepository.findByConversationIdOrderByJoinTimeAsc(conversationId))
                .thenReturn(Arrays.asList(
                        new ConversationParticipant(UUID.randomUUID(), conversationId, senderId, ConversationParticipant.Role.OWNER),
                        new ConversationParticipant(UUID.randomUUID(), conversationId, otherId, ConversationParticipant.Role.MEMBER)
                ));
        when(orchestratorClient.generateReply(anyString(), anyString(), anyList(), anyList(), any()))
                .thenReturn("");

        ArgumentCaptor<List<MessageContext>> contextCaptor = ArgumentCaptor.forClass(List.class);
        when(aiClient.generateReply(anyString(), anyString(), contextCaptor.capture()))
                .thenReturn("");

        MessageSendRequest request = new MessageSendRequest();
        request.setSenderId(senderId);
        request.setContent("trigger");
        request.setGenerateAiReply(true);
        request.setPersonaSummary("persona");

        messageService.sendMessage(conversationId, request);

        List<MessageContext> captured = contextCaptor.getValue();
        assertEquals(2, captured.size());
        assertEquals("first", captured.get(0).getContent());
        assertEquals("user", captured.get(0).getRole());
        assertEquals("second", captured.get(1).getContent());
        assertEquals("assistant", captured.get(1).getRole());
    }
}
