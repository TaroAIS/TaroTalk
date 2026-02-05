package com.tarotalk.ai.service;

import com.tarotalk.ai.api.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;

    public AiService(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.promptBuilder = new PromptBuilder();
    }

    public PersonaGenerateResponse generatePersona(PersonaGenerateRequest request) {
        String prompt = promptBuilder.buildPersonaPrompt(request.getDescription());
        String raw = llmClient.generateText("You are a persona generator.", prompt);
        String summary = summarize(raw);
        Map<String, Object> traits = new HashMap<>();
        traits.put("keywords", extractKeywords(raw));
        return new PersonaGenerateResponse(summary, traits, raw);
    }

    public AiReplyResponse generateReply(AiReplyRequest request) {
        String prompt = promptBuilder.buildReplyPrompt(request.getPersonaSummary(), request.getMessages());
        String reply = llmClient.generateText("You are an AI chat agent.", prompt);
        return new AiReplyResponse(reply, Collections.emptyList());
    }

    public AiFeedResponse generateFeed(AiFeedRequest request) {
        String prompt = promptBuilder.buildFeedPrompt(request.getPersonaSummary(), request.getTopic());
        String content = llmClient.generateText("You are a social feed generator.", prompt);
        return new AiFeedResponse(content);
    }

    private String summarize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.length() > 240 ? trimmed.substring(0, 240) : trimmed;
    }

    private List<String> extractKeywords(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String[] tokens = raw.replaceAll("[^a-zA-Z0-9 ]", " ").split("\\s+");
        List<String> keywords = new java.util.ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 4 && keywords.size() < 6) {
                keywords.add(token.toLowerCase());
            }
        }
        return keywords;
    }
}
