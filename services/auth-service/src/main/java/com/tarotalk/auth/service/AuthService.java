package com.tarotalk.auth.service;

import com.tarotalk.auth.api.AuthResponse;
import com.tarotalk.auth.api.LoginRequest;
import com.tarotalk.auth.api.RefreshRequest;
import com.tarotalk.auth.api.RegisterRequest;
import com.tarotalk.auth.domain.AuthUser;
import com.tarotalk.auth.repo.AuthUserRepository;
import com.tarotalk.common.exception.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileClient userProfileClient;

    public AuthService(AuthUserRepository authUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserProfileClient userProfileClient) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userProfileClient = userProfileClient;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() == null && request.getPhone() == null) {
            throw new ApiException("VALIDATION_ERROR", "email or phone is required");
        }
        if (request.getEmail() != null && authUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException("ALREADY_EXISTS", "email already registered");
        }
        if (request.getPhone() != null && authUserRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new ApiException("ALREADY_EXISTS", "phone already registered");
        }
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser(userId, request.getEmail(), request.getPhone(),
                passwordEncoder.encode(request.getPassword()), 1);
        authUserRepository.save(user);
        userProfileClient.createProfile(userId, request);
        String token = jwtService.generateToken(userId);
        return new AuthResponse(userId, token, jwtService.getExpirationInstant());
    }

    public AuthResponse login(LoginRequest request) {
        Optional<AuthUser> userOpt = Optional.empty();
        if (request.getEmail() != null) {
            userOpt = authUserRepository.findByEmail(request.getEmail());
        } else if (request.getPhone() != null) {
            userOpt = authUserRepository.findByPhone(request.getPhone());
        }
        AuthUser user = userOpt.orElseThrow(() -> new ApiException("NOT_FOUND", "user not found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException("AUTH_FAILED", "invalid credentials");
        }
        String token = jwtService.generateToken(user.getUserId());
        return new AuthResponse(user.getUserId(), token, jwtService.getExpirationInstant());
    }

    public AuthResponse refresh(RefreshRequest request) {
        UUID userId = jwtService.parseUserId(request.getAccessToken());
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "user not found"));
        String token = jwtService.generateToken(user.getUserId());
        return new AuthResponse(user.getUserId(), token, jwtService.getExpirationInstant());
    }
}
