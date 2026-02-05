package com.tarotalk.user.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacts")
public class Contact {
    @Id
    @Column(name = "contact_id", nullable = false, updatable = false)
    private UUID contactId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "contact_user_id", nullable = false)
    private UUID contactUserId;

    @Column(name = "group_name")
    private String groupName;

    @Column(nullable = false)
    private boolean blocked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Contact() {
    }

    public Contact(UUID contactId, UUID userId, UUID contactUserId) {
        this.contactId = contactId;
        this.userId = userId;
        this.contactUserId = contactUserId;
        this.blocked = false;
        this.createdAt = Instant.now();
    }

    public UUID getContactId() {
        return contactId;
    }

    public void setContactId(UUID contactId) {
        this.contactId = contactId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(UUID contactUserId) {
        this.contactUserId = contactUserId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
