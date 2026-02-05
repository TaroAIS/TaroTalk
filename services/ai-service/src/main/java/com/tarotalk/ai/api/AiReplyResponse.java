package com.tarotalk.ai.api;

import java.util.List;

public class AiReplyResponse {
    private String replyText;
    private List<String> memoryIds;

    public AiReplyResponse() {
    }

    public AiReplyResponse(String replyText, List<String> memoryIds) {
        this.replyText = replyText;
        this.memoryIds = memoryIds;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public List<String> getMemoryIds() {
        return memoryIds;
    }

    public void setMemoryIds(List<String> memoryIds) {
        this.memoryIds = memoryIds;
    }
}
