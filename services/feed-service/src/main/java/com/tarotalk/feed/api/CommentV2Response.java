package com.tarotalk.feed.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarotalk.feed.domain.FeedInteraction;

public class CommentV2Response {
    private FeedInteraction interaction;
    private String eventId;

    public CommentV2Response() {
    }

    public CommentV2Response(FeedInteraction interaction, String eventId) {
        this.interaction = interaction;
        this.eventId = eventId;
    }

    public FeedInteraction getInteraction() {
        return interaction;
    }

    @JsonProperty("event_id")
    public String getEventId() {
        return eventId;
    }
}
