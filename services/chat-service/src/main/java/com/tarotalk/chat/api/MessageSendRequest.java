package com.tarotalk.chat.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class MessageSendRequest {
    @NotNull
    private UUID senderId;

    @NotBlank
    private String content;

    private String type = "text";
    private String replyToMessageId;

    private boolean generateAiReply;
    private String personaSummary;
    private List<MessageContext> context;

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public boolean isGenerateAiReply() {
        return generateAiReply;
    }

    public void setGenerateAiReply(boolean generateAiReply) {
        this.generateAiReply = generateAiReply;
    }

    public String getPersonaSummary() {
        return personaSummary;
    }

    public void setPersonaSummary(String personaSummary) {
        this.personaSummary = personaSummary;
    }

    public List<MessageContext> getContext() {
        return context;
    }

    public void setContext(List<MessageContext> context) {
        this.context = context;
    }
}
