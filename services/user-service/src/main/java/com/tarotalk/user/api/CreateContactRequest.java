package com.tarotalk.user.api;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class CreateContactRequest {
    @NotNull
    private UUID userId;
    @NotNull
    private UUID contactUserId;
    private String groupName;
    private Boolean blocked;

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

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
}
