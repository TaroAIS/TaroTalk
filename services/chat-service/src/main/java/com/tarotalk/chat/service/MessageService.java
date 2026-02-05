package com.tarotalk.chat.service;

import com.tarotalk.chat.api.MessageContext;
import com.tarotalk.chat.api.MessageSendRequest;
import com.tarotalk.chat.domain.ChatMessage;
import com.tarotalk.chat.domain.Conversation;
import com.tarotalk.chat.repo.ChatMessageRepository;
import com.tarotalk.chat.repo.ConversationParticipantRepository;
import com.tarotalk.chat.repo.ConversationRepository;
import com.tarotalk.chat.websocket.WebSocketPublisher;
import com.tarotalk.common.api.PageResponse;
import com.tarotalk.common.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final AiClient aiClient;
    private final OrchestratorClient orchestratorClient;
    private final WebSocketPublisher webSocketPublisher;

    public MessageService(ChatMessageRepository chatMessageRepository,
                          ConversationRepository conversationRepository,
                          ConversationParticipantRepository participantRepository,
                          AiClient aiClient,
                          OrchestratorClient orchestratorClient,
                          WebSocketPublisher webSocketPublisher) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.aiClient = aiClient;
        this.orchestratorClient = orchestratorClient;
        this.webSocketPublisher = webSocketPublisher;
    }

    public ChatMessage sendMessage(UUID conversationId, MessageSendRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "conversation not found"));
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(request.getSenderId());
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setReplyToMessageId(request.getReplyToMessageId());
        message.setSentAt(Instant.now());
        ChatMessage saved = chatMessageRepository.save(message);
        conversation.setLastMessageId(saved.getMessageId());
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        webSocketPublisher.publish(conversationId.toString(), saved);

        if (request.isGenerateAiReply()) {
            java.util.List<java.util.UUID> participants = participantRepository.findByConversationId(conversationId)
                    .stream()
                    .map(com.tarotalk.chat.domain.ConversationParticipant::getUserId)
                    .collect(java.util.stream.Collectors.toList());
            String replyText = orchestratorClient.generateReply(conversationId.toString(), request.getPersonaSummary(), safeContext(request.getContext()), participants);
            if (replyText == null || replyText.isEmpty()) {
                replyText = aiClient.generateReply(request.getPersonaSummary(), conversationId.toString(), safeContext(request.getContext()));
            }
            if (replyText != null && !replyText.isEmpty()) {
                ChatMessage aiMessage = new ChatMessage();
                aiMessage.setConversationId(conversationId);
                aiMessage.setSenderId(request.getSenderId());
                aiMessage.setType("text");
                aiMessage.setContent(replyText);
                aiMessage.setSentAt(Instant.now());
                ChatMessage aiSaved = chatMessageRepository.save(aiMessage);
                conversation.setLastMessageId(aiSaved.getMessageId());
                conversation.setUpdatedAt(Instant.now());
                conversationRepository.save(conversation);
                webSocketPublisher.publish(conversationId.toString(), aiSaved);
            }
        }
        return saved;
    }

    public PageResponse<ChatMessage> listMessages(UUID conversationId, int page, int size) {
        Page<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderBySentAtDesc(conversationId, PageRequest.of(page, size));
        return new PageResponse<>(messages.getContent(), page, size, messages.getTotalElements());
    }

    public ChatMessage deleteMessage(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "message not found"));
        message.setContent("[deleted]");
        return chatMessageRepository.save(message);
    }

    public ChatMessage markRead(String messageId, UUID readerId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "message not found"));
        message.setReadAt(Instant.now());
        ChatMessage saved = chatMessageRepository.save(message);
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("messageId", messageId);
        payload.put("userId", readerId.toString());
        webSocketPublisher.publishEvent(saved.getConversationId().toString(), "read", payload);
        return saved;
    }

    public void publishTyping(UUID conversationId, UUID userId, boolean typing) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId.toString());
        payload.put("typing", typing);
        webSocketPublisher.publishEvent(conversationId.toString(), "typing", payload);
    }

    private List<MessageContext> safeContext(List<MessageContext> context) {
        return context == null ? java.util.Collections.emptyList() : context;
    }
}
