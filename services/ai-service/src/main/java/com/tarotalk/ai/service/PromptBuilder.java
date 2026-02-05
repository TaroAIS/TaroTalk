package com.tarotalk.ai.service;

import com.tarotalk.ai.api.MessageContext;

import java.util.List;

public class PromptBuilder {
    public String buildPersonaPrompt(String description) {
        return "Generate a concise persona summary and key traits based on: " + description;
    }

    public String buildReplyPrompt(String personaSummary, List<MessageContext> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("Persona summary: ").append(personaSummary).append("\n");
        builder.append("Conversation:\n");
        for (MessageContext message : messages) {
            builder.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }
        builder.append("Respond in character in one short paragraph.");
        return builder.toString();
    }

    public String buildFeedPrompt(String personaSummary, String topic) {
        return "Write a short social feed post in character. Persona: " + personaSummary + ". Topic: " + topic;
    }
}
