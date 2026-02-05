package com.tarotalk.user.service;

import com.tarotalk.common.exception.ApiException;
import com.tarotalk.user.api.CreateUserRequest;
import com.tarotalk.user.api.UserUpdateRequest;
import com.tarotalk.user.domain.UserProfile;
import com.tarotalk.user.repo.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {
    private final UserProfileRepository userProfileRepository;

    public UserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile createProfile(CreateUserRequest request) {
        UUID userId = request.getUserId() == null ? UUID.randomUUID() : request.getUserId();
        if (userProfileRepository.existsById(userId)) {
            throw new ApiException("ALREADY_EXISTS", "user profile already exists");
        }
        String nickname = request.getNickname() == null ? "New User" : request.getNickname();
        UserProfile profile = new UserProfile(userId, nickname, request.getAvatarUrl());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        return userProfileRepository.save(profile);
    }

    public UserProfile getProfile(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "user not found"));
    }

    public UserProfile updateProfile(UUID userId, UserUpdateRequest request) {
        UserProfile profile = getProfile(userId);
        if (request.getNickname() != null) {
            profile.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        profile.setUpdatedAt(Instant.now());
        return userProfileRepository.save(profile);
    }
}
