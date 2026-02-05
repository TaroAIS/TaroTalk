package com.tarotalk.auth.repo;

import com.tarotalk.auth.domain.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByPhone(String phone);
}
