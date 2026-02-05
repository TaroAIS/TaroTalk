package com.tarotalk.ai.api;

public class AiFeedResponse {
    private String content;

    public AiFeedResponse() {
    }

    public AiFeedResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
