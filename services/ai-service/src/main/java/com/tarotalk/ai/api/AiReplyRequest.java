package com.tarotalk.ai.api;

import java.util.List;

public class AiReplyRequest {
    private String personaSummary;
    private String conversationId;
    private List<MessageContext> messages;

    public String getPersonaSummary() {
        return personaSummary;
    }

    public void setPersonaSummary(String personaSummary) {
        this.personaSummary = personaSummary;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<MessageContext> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageContext> messages) {
        this.messages = messages;
    }
}
