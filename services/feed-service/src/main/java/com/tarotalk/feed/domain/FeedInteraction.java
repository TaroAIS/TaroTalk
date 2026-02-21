package com.tarotalk.feed.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "feed_interactions",
        indexes = {
                @Index(name = "idx_feed_interaction_feed_created", columnList = "feed_id,created_at"),
                @Index(name = "idx_feed_interaction_user_type_created", columnList = "user_id,type,created_at")
        }
)
public class FeedInteraction {
    public enum Type {
        LIKE,
        COMMENT,
        SHARE
    }

    @Id
    @org.hibernate.annotations.Type(type = "uuid-char")
    @Column(name = "interaction_id", nullable = false, updatable = false)
    private UUID interactionId;

    @Column(name = "feed_id", nullable = false)
    @org.hibernate.annotations.Type(type = "uuid-char")
    private UUID feedId;

    @Column(name = "user_id", nullable = false)
    @org.hibernate.annotations.Type(type = "uuid-char")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FeedInteraction() {
    }

    public FeedInteraction(UUID interactionId, UUID feedId, UUID userId, Type type, String content) {
        this.interactionId = interactionId;
        this.feedId = feedId;
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(UUID interactionId) {
        this.interactionId = interactionId;
    }

    public UUID getFeedId() {
        return feedId;
    }

    public void setFeedId(UUID feedId) {
        this.feedId = feedId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
