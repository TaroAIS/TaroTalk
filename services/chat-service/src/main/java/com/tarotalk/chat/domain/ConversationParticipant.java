package com.tarotalk.chat.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants")
public class ConversationParticipant {
    public enum Role {
        MEMBER,
        ADMIN,
        OWNER
    }

    @Id
    @Type(type = "uuid-char")
    @Column(name = "participant_id", nullable = false, updatable = false)
    private UUID participantId;

    @Column(name = "conversation_id", nullable = false)
    @Type(type = "uuid-char")
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    @Type(type = "uuid-char")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "join_time", nullable = false)
    private Instant joinTime;

    @Column(name = "last_read_message_id")
    private String lastReadMessageId;

    public ConversationParticipant() {
    }

    public ConversationParticipant(UUID participantId, UUID conversationId, UUID userId, Role role) {
        this.participantId = participantId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.joinTime = Instant.now();
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Instant joinTime) {
        this.joinTime = joinTime;
    }

    public String getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(String lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
