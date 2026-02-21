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
import com.tarotalk.common.web.TraceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MessageService {
    private static final int CONTEXT_LIMIT = 20;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final AiClient aiClient;
    private final OrchestratorClient orchestratorClient;
    private final WebSocketPublisher webSocketPublisher;
    private final ChatEventPublisher chatEventPublisher;

    public MessageService(ChatMessageRepository chatMessageRepository,
                          ConversationRepository conversationRepository,
                          ConversationParticipantRepository participantRepository,
                          AiClient aiClient,
                          OrchestratorClient orchestratorClient,
                          WebSocketPublisher webSocketPublisher,
                          ChatEventPublisher chatEventPublisher) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.aiClient = aiClient;
        this.orchestratorClient = orchestratorClient;
        this.webSocketPublisher = webSocketPublisher;
        this.chatEventPublisher = chatEventPublisher;
    }

    public ChatMessage sendMessage(UUID conversationId, MessageSendRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "conversation not found"));
        List<UUID> participants = participantRepository.findByConversationIdOrderByJoinTimeAsc(conversationId)
                .stream()
                .map(com.tarotalk.chat.domain.ConversationParticipant::getUserId)
                .collect(java.util.stream.Collectors.toList());
        if (!participants.contains(request.getSenderId())) {
            throw new ApiException("FORBIDDEN_NOT_PARTICIPANT", "sender is not conversation participant");
        }
        String requestTraceId = TraceContext.currentTraceIdOrRandom();

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(request.getSenderId());
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setReplyToMessageId(request.getReplyToMessageId());
        message.setSentAt(Instant.now());
        message.setRole("user");
        message.setRound(0);
        message.setSource("USER");
        message.setTraceId(requestTraceId);
        ChatMessage saved = chatMessageRepository.save(message);
        conversation.setLastMessageId(saved.getMessageId());
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        webSocketPublisher.publish(conversationId.toString(), saved);
        chatEventPublisher.publishMessageSaved(saved, requestTraceId);

        if (request.isGenerateAiReply()) {
            List<MessageContext> context = buildContextIfMissing(conversationId, request.getSenderId(), request.getContext());
            OrchestratorReply orchestratorReply = orchestratorClient.generateReply(
                    conversationId.toString(),
                    request.getPersonaSummary(),
                    context,
                    participants,
                    request.getSenderId(),
                    request.getWorldId(),
                    request.getContextWindow(),
                    request.getIntent()
            );
            boolean persistedByTurn = persistOrchestratorTurns(
                    conversation,
                    participants,
                    request.getSenderId(),
                    orchestratorReply,
                    requestTraceId
            );
            if (!persistedByTurn) {
                String replyText = orchestratorReply == null ? "" : orchestratorReply.getReply();
                if (replyText == null || replyText.isEmpty()) {
                    replyText = aiClient.generateReply(request.getPersonaSummary(), conversationId.toString(), context);
                }
                if (replyText != null && !replyText.isEmpty()) {
                    UUID fallbackSender = chooseFallbackAiSender(participants, request.getSenderId());
                    ChatMessage aiMessage = new ChatMessage();
                    aiMessage.setConversationId(conversationId);
                    aiMessage.setSenderId(fallbackSender);
                    aiMessage.setType("text");
                    aiMessage.setContent(replyText);
                    aiMessage.setSentAt(Instant.now());
                    aiMessage.setRole("assistant");
                    aiMessage.setRound(1);
                    aiMessage.setSource("AGENT");
                    aiMessage.setTraceId(resolveTraceId(orchestratorReply, requestTraceId));
                    ChatMessage aiSaved = chatMessageRepository.save(aiMessage);
                    conversation.setLastMessageId(aiSaved.getMessageId());
                    conversation.setUpdatedAt(Instant.now());
                    conversationRepository.save(conversation);
                    webSocketPublisher.publish(conversationId.toString(), aiSaved);
                    chatEventPublisher.publishMessageSaved(aiSaved, aiSaved.getTraceId());
                }
            }
        }
        return saved;
    }

    private boolean persistOrchestratorTurns(Conversation conversation,
                                             List<UUID> participants,
                                             UUID senderId,
                                             OrchestratorReply orchestratorReply,
                                             String defaultTraceId) {
        if (orchestratorReply == null || orchestratorReply.getTurns() == null || orchestratorReply.getTurns().isEmpty()) {
            return false;
        }

        String traceId = resolveTraceId(orchestratorReply, defaultTraceId);
        ChatMessage lastSaved = null;
        for (OrchestratorReply.Turn turn : orchestratorReply.getTurns()) {
            if (turn == null || turn.getContent() == null || turn.getContent().trim().isEmpty()) {
                continue;
            }
            UUID turnSender = parseUuid(turn.getUserId());
            if (turnSender == null) {
                turnSender = chooseFallbackAiSender(participants, senderId);
            }
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setConversationId(conversation.getConversationId());
            aiMessage.setSenderId(turnSender);
            aiMessage.setType("text");
            aiMessage.setContent(turn.getContent());
            aiMessage.setSentAt(Instant.now());
            aiMessage.setRole(turn.getRole());
            aiMessage.setRound(turn.getRound());
            aiMessage.setSource("AGENT");
            aiMessage.setTraceId(traceId);
            String effectRef = turn.getEffectRef();
            if (effectRef == null || effectRef.trim().isEmpty()) {
                effectRef = resolveEffectRef(orchestratorReply.getStateEffects(), turn);
            }
            aiMessage.setEffectRef(effectRef);
            lastSaved = chatMessageRepository.save(aiMessage);
            webSocketPublisher.publish(conversation.getConversationId().toString(), lastSaved);
            chatEventPublisher.publishMessageSaved(lastSaved, traceId);
        }

        if (lastSaved == null) {
            return false;
        }
        conversation.setLastMessageId(lastSaved.getMessageId());
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        return true;
    }

    private String resolveEffectRef(List<Map<String, Object>> stateEffects, OrchestratorReply.Turn turn) {
        if (stateEffects == null || stateEffects.isEmpty() || turn == null) {
            return null;
        }
        for (Map<String, Object> effect : stateEffects) {
            if (effect == null) {
                continue;
            }
            Object effectType = effect.get("effect_type");
            if (effectType == null || !"CHAT_MESSAGE".equals(String.valueOf(effectType))) {
                continue;
            }
            Object round = effect.get("round");
            Object role = effect.get("role");
            Object content = effect.get("content");
            if (round instanceof Number && ((Number) round).intValue() != turn.getRound()) {
                continue;
            }
            if (role != null && turn.getRole() != null && !turn.getRole().equals(String.valueOf(role))) {
                continue;
            }
            if (content != null && turn.getContent() != null && !turn.getContent().equals(String.valueOf(content))) {
                continue;
            }
            Object ref = effect.get("effect_ref");
            if (ref == null) {
                ref = effect.get("event_id");
            }
            if (ref != null) {
                return String.valueOf(ref);
            }
            return "CHAT_MESSAGE:" + turn.getRound() + ":" + (turn.getRole() == null ? "unknown" : turn.getRole());
        }
        return null;
    }

    private String resolveTraceId(OrchestratorReply orchestratorReply, String fallback) {
        if (orchestratorReply != null && orchestratorReply.getTraceId() != null && !orchestratorReply.getTraceId().trim().isEmpty()) {
            return orchestratorReply.getTraceId();
        }
        return fallback;
    }

    private UUID chooseFallbackAiSender(List<UUID> participants, UUID senderId) {
        if (participants == null || participants.isEmpty()) {
            return senderId;
        }
        for (UUID participant : participants) {
            if (participant != null && !participant.equals(senderId)) {
                return participant;
            }
        }
        return senderId;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private List<MessageContext> buildContextIfMissing(UUID conversationId, UUID senderId, List<MessageContext> context) {
        if (context != null && !context.isEmpty()) {
            return context;
        }
        Page<ChatMessage> history = chatMessageRepository.findByConversationIdOrderBySentAtDesc(
                conversationId,
                PageRequest.of(0, CONTEXT_LIMIT)
        );
        List<ChatMessage> messages = new ArrayList<>(history.getContent());
        java.util.Collections.reverse(messages);
        List<MessageContext> built = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message.getContent() == null) {
                continue;
            }
            MessageContext ctx = new MessageContext();
            ctx.setContent(message.getContent());
            String role = message.getRole();
            if (role == null || role.trim().isEmpty()) {
                role = message.getSenderId() != null && message.getSenderId().equals(senderId) ? "user" : "assistant";
            }
            ctx.setRole(role);
            built.add(ctx);
        }
        return built;
    }
}
