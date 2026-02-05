package com.tarotalk.user.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.user.domain.UserProfile;
import com.tarotalk.user.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<UserProfileResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserProfile profile = userService.createProfile(request);
        return ApiResponse.ok(UserProfileResponse.from(profile));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> get(@PathVariable UUID userId) {
        return ApiResponse.ok(UserProfileResponse.from(userService.getProfile(userId)));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserProfileResponse> update(@PathVariable UUID userId,
                                                   @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(UserProfileResponse.from(userService.updateProfile(userId, request)));
    }
}
