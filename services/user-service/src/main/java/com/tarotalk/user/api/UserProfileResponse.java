package com.tarotalk.user.api;

import com.tarotalk.user.domain.UserProfile;

import java.time.Instant;
import java.util.UUID;

public class UserProfileResponse {
    private UUID userId;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String email;
    private int status;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserProfileResponse from(UserProfile profile) {
        UserProfileResponse response = new UserProfileResponse();
        response.userId = profile.getUserId();
        response.nickname = profile.getNickname();
        response.avatarUrl = profile.getAvatarUrl();
        response.phone = profile.getPhone();
        response.email = profile.getEmail();
        response.status = profile.getStatus();
        response.createdAt = profile.getCreatedAt();
        response.updatedAt = profile.getUpdatedAt();
        return response;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public int getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
