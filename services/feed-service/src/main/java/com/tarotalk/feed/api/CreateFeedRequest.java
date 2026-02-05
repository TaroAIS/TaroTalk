package com.tarotalk.feed.api;

import com.tarotalk.feed.domain.Feed;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

public class CreateFeedRequest {
    @NotNull
    private UUID authorId;

    @NotBlank
    private String content;

    private String mediaUrls;
    private String location;
    private String topics;
    private Feed.Visibility visibility = Feed.Visibility.PUBLIC;

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public Feed.Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Feed.Visibility visibility) {
        this.visibility = visibility;
    }
}
