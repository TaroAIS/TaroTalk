package com.tarotalk.chat.api;

import com.tarotalk.chat.domain.Conversation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ConversationResponse {
    private UUID conversationId;
    private Conversation.Type type;
    private String title;
    private String lastMessageId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<UUID> participants;

    public static ConversationResponse from(Conversation conversation, List<UUID> participants) {
        ConversationResponse response = new ConversationResponse();
        response.conversationId = conversation.getConversationId();
        response.type = conversation.getType();
        response.title = conversation.getTitle();
        response.lastMessageId = conversation.getLastMessageId();
        response.createdAt = conversation.getCreatedAt();
        response.updatedAt = conversation.getUpdatedAt();
        response.participants = participants;
        return response;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public Conversation.Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<UUID> getParticipants() {
        return participants;
    }
}
