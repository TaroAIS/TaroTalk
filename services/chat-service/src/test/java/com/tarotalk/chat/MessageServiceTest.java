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
import com.tarotalk.chat.service.ChatEventPublisher;
import com.tarotalk.chat.service.MessageService;
import com.tarotalk.chat.service.OrchestratorClient;
import com.tarotalk.chat.service.OrchestratorReply;
import com.tarotalk.chat.websocket.WebSocketPublisher;
import com.tarotalk.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        ChatEventPublisher chatEventPublisher = mock(ChatEventPublisher.class);

        MessageService messageService = new MessageService(
                chatMessageRepository,
                conversationRepository,
                participantRepository,
                aiClient,
                orchestratorClient,
                webSocketPublisher,
                chatEventPublisher
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
        when(orchestratorClient.generateReply(anyString(), anyString(), anyList(), anyList(), any(), any(), any(), any()))
                .thenReturn(new OrchestratorReply());

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

    @Test
    void persistStructuredTurnsFromOrchestrator() {
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ConversationParticipantRepository participantRepository = mock(ConversationParticipantRepository.class);
        AiClient aiClient = mock(AiClient.class);
        OrchestratorClient orchestratorClient = mock(OrchestratorClient.class);
        WebSocketPublisher webSocketPublisher = mock(WebSocketPublisher.class);
        ChatEventPublisher chatEventPublisher = mock(ChatEventPublisher.class);

        MessageService messageService = new MessageService(
                chatMessageRepository,
                conversationRepository,
                participantRepository,
                aiClient,
                orchestratorClient,
                webSocketPublisher,
                chatEventPublisher
        );

        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID aiId = UUID.randomUUID();

        Conversation conversation = new Conversation(conversationId, Conversation.Type.ONE_ON_ONE, "test");
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            return message;
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(participantRepository.findByConversationIdOrderByJoinTimeAsc(conversationId))
                .thenReturn(Arrays.asList(
                        new ConversationParticipant(UUID.randomUUID(), conversationId, senderId, ConversationParticipant.Role.OWNER),
                        new ConversationParticipant(UUID.randomUUID(), conversationId, aiId, ConversationParticipant.Role.MEMBER)
                ));
        when(chatMessageRepository.findByConversationIdOrderBySentAtDesc(eq(conversationId), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        OrchestratorReply reply = new OrchestratorReply();
        OrchestratorReply.Turn turn1 = new OrchestratorReply.Turn();
        turn1.setRound(1);
        turn1.setRole("self-agent");
        turn1.setUserId(senderId.toString());
        turn1.setContent("self turn");
        OrchestratorReply.Turn turn2 = new OrchestratorReply.Turn();
        turn2.setRound(2);
        turn2.setRole("friend");
        turn2.setUserId(aiId.toString());
        turn2.setContent("friend turn");
        reply.setTurns(Arrays.asList(turn1, turn2));
        when(orchestratorClient.generateReply(anyString(), anyString(), anyList(), anyList(), any(), any(), any(), any()))
                .thenReturn(reply);

        MessageSendRequest request = new MessageSendRequest();
        request.setSenderId(senderId);
        request.setContent("trigger");
        request.setGenerateAiReply(true);
        request.setPersonaSummary("persona");

        messageService.sendMessage(conversationId, request);

        ArgumentCaptor<ChatMessage> saveCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, atLeast(3)).save(saveCaptor.capture());
        List<ChatMessage> saved = saveCaptor.getAllValues();
        assertEquals("trigger", saved.get(0).getContent());
        assertEquals("self turn", saved.get(1).getContent());
        assertEquals(senderId, saved.get(1).getSenderId());
        assertEquals("friend turn", saved.get(2).getContent());
        assertEquals(aiId, saved.get(2).getSenderId());
        verify(aiClient, never()).generateReply(anyString(), anyString(), anyList());
    }

    @Test
    void fallbackReplyUsesNonSenderParticipant() {
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ConversationParticipantRepository participantRepository = mock(ConversationParticipantRepository.class);
        AiClient aiClient = mock(AiClient.class);
        OrchestratorClient orchestratorClient = mock(OrchestratorClient.class);
        WebSocketPublisher webSocketPublisher = mock(WebSocketPublisher.class);
        ChatEventPublisher chatEventPublisher = mock(ChatEventPublisher.class);

        MessageService messageService = new MessageService(
                chatMessageRepository,
                conversationRepository,
                participantRepository,
                aiClient,
                orchestratorClient,
                webSocketPublisher,
                chatEventPublisher
        );

        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID aiId = UUID.randomUUID();

        Conversation conversation = new Conversation(conversationId, Conversation.Type.ONE_ON_ONE, "test");
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            return message;
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(participantRepository.findByConversationIdOrderByJoinTimeAsc(conversationId))
                .thenReturn(Arrays.asList(
                        new ConversationParticipant(UUID.randomUUID(), conversationId, senderId, ConversationParticipant.Role.OWNER),
                        new ConversationParticipant(UUID.randomUUID(), conversationId, aiId, ConversationParticipant.Role.MEMBER)
                ));
        when(chatMessageRepository.findByConversationIdOrderBySentAtDesc(eq(conversationId), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        OrchestratorReply reply = new OrchestratorReply();
        reply.setReply("single fallback reply");
        when(orchestratorClient.generateReply(anyString(), anyString(), anyList(), anyList(), any(), any(), any(), any()))
                .thenReturn(reply);

        MessageSendRequest request = new MessageSendRequest();
        request.setSenderId(senderId);
        request.setContent("trigger");
        request.setGenerateAiReply(true);
        request.setPersonaSummary("persona");

        messageService.sendMessage(conversationId, request);

        ArgumentCaptor<ChatMessage> saveCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, atLeast(2)).save(saveCaptor.capture());
        List<ChatMessage> saved = saveCaptor.getAllValues();
        ChatMessage fallback = saved.get(saved.size() - 1);
        assertEquals("single fallback reply", fallback.getContent());
        assertEquals(aiId, fallback.getSenderId());
        verify(aiClient, never()).generateReply(anyString(), anyString(), anyList());
    }

    @Test
    void sendMessageRejectsNonParticipantSender() {
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ConversationParticipantRepository participantRepository = mock(ConversationParticipantRepository.class);
        AiClient aiClient = mock(AiClient.class);
        OrchestratorClient orchestratorClient = mock(OrchestratorClient.class);
        WebSocketPublisher webSocketPublisher = mock(WebSocketPublisher.class);
        ChatEventPublisher chatEventPublisher = mock(ChatEventPublisher.class);

        MessageService messageService = new MessageService(
                chatMessageRepository,
                conversationRepository,
                participantRepository,
                aiClient,
                orchestratorClient,
                webSocketPublisher,
                chatEventPublisher
        );

        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        Conversation conversation = new Conversation(conversationId, Conversation.Type.ONE_ON_ONE, "test");
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationIdOrderByJoinTimeAsc(conversationId))
                .thenReturn(Collections.singletonList(
                        new ConversationParticipant(UUID.randomUUID(), conversationId, otherId, ConversationParticipant.Role.OWNER)
                ));

        MessageSendRequest request = new MessageSendRequest();
        request.setSenderId(senderId);
        request.setContent("unauthorized");
        request.setGenerateAiReply(false);

        assertThrows(ApiException.class, () -> messageService.sendMessage(conversationId, request));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
}
