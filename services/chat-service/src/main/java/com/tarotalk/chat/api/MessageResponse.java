package com.tarotalk.chat.api;

import com.tarotalk.chat.domain.ChatMessage;

import java.time.Instant;
import java.util.UUID;

public class MessageResponse {
    private String messageId;
    private UUID conversationId;
    private UUID senderId;
    private String type;
    private String content;
    private String mediaUrl;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private String replyToMessageId;

    public static MessageResponse from(ChatMessage message) {
        MessageResponse response = new MessageResponse();
        response.messageId = message.getMessageId();
        response.conversationId = message.getConversationId();
        response.senderId = message.getSenderId();
        response.type = message.getType();
        response.content = message.getContent();
        response.mediaUrl = message.getMediaUrl();
        response.sentAt = message.getSentAt();
        response.deliveredAt = message.getDeliveredAt();
        response.readAt = message.getReadAt();
        response.replyToMessageId = message.getReplyToMessageId();
        return response;
    }

    public String getMessageId() {
        return messageId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }
}
