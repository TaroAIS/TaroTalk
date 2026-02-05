package com.tarotalk.ai.api;

import java.util.Map;

public class PersonaGenerateResponse {
    private String summary;
    private Map<String, Object> traits;
    private String rawText;

    public PersonaGenerateResponse() {
    }

    public PersonaGenerateResponse(String summary, Map<String, Object> traits, String rawText) {
        this.summary = summary;
        this.traits = traits;
        this.rawText = rawText;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Map<String, Object> getTraits() {
        return traits;
    }

    public void setTraits(Map<String, Object> traits) {
        this.traits = traits;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
