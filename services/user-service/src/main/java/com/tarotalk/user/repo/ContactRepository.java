package com.tarotalk.user.repo;

import com.tarotalk.user.domain.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Page<Contact> findByUserId(UUID userId, Pageable pageable);
    Page<Contact> findByUserIdAndGroupNameContainingIgnoreCase(UUID userId, String groupName, Pageable pageable);
}
