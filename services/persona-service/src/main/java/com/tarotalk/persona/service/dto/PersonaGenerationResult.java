package com.tarotalk.persona.service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

public class PersonaGenerationResult {
    private final String summary;
    private final Map<String, Object> traits;

    public PersonaGenerationResult(String summary, Map<String, Object> traits) {
        this.summary = summary;
        this.traits = traits;
    }

    public static PersonaGenerationResult stub(String description) {
        return new PersonaGenerationResult(description, Collections.emptyMap());
    }

    public static PersonaGenerationResult fromResponse(Map response, String fallback) {
        if (response == null) {
            return stub(fallback);
        }
        Object data = response.get("data");
        if (data instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) data;
            String summary = String.valueOf(dataMap.getOrDefault("summary", fallback));
            Object traits = dataMap.get("traits");
            if (traits instanceof Map) {
                return new PersonaGenerationResult(summary, (Map<String, Object>) traits);
            }
            return new PersonaGenerationResult(summary, Collections.emptyMap());
        }
        return stub(fallback);
    }

    public String getSummary() {
        return summary;
    }

    public Map<String, Object> getTraits() {
        return traits;
    }

    public String toTraitsJson() {
        try {
            return new ObjectMapper().writeValueAsString(traits);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
