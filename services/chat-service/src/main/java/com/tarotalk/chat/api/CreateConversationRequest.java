package com.tarotalk.chat.api;

import com.tarotalk.chat.domain.Conversation;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class CreateConversationRequest {
    @NotNull
    private Conversation.Type type;

    private String title;

    @NotEmpty
    private List<String> participantIds;

    public Conversation.Type getType() {
        return type;
    }

    public void setType(Conversation.Type type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }
}
