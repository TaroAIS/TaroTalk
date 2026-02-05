package com.tarotalk.user.api;

import com.tarotalk.user.domain.Contact;

import java.time.Instant;
import java.util.UUID;

public class ContactResponse {
    private UUID contactId;
    private UUID userId;
    private UUID contactUserId;
    private String groupName;
    private boolean blocked;
    private Instant createdAt;

    public static ContactResponse from(Contact contact) {
        ContactResponse response = new ContactResponse();
        response.contactId = contact.getContactId();
        response.userId = contact.getUserId();
        response.contactUserId = contact.getContactUserId();
        response.groupName = contact.getGroupName();
        response.blocked = contact.isBlocked();
        response.createdAt = contact.getCreatedAt();
        return response;
    }

    public UUID getContactId() {
        return contactId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getContactUserId() {
        return contactUserId;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
