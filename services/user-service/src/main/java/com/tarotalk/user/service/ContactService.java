package com.tarotalk.user.service;

import com.tarotalk.common.api.PageResponse;
import com.tarotalk.common.exception.ApiException;
import com.tarotalk.user.api.ContactGroupRequest;
import com.tarotalk.user.domain.Contact;
import com.tarotalk.user.repo.ContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ContactService {
    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public PageResponse<Contact> listContacts(UUID userId, int page, int size) {
        Page<Contact> result = contactRepository.findByUserId(userId, PageRequest.of(page, size));
        return new PageResponse<>(result.getContent(), page, size, result.getTotalElements());
    }

    public PageResponse<Contact> searchContacts(UUID userId, String keyword, int page, int size) {
        Page<Contact> result = contactRepository.findByUserIdAndGroupNameContainingIgnoreCase(userId, keyword, PageRequest.of(page, size));
        return new PageResponse<>(result.getContent(), page, size, result.getTotalElements());
    }

    public Contact updateGroup(UUID contactId, ContactGroupRequest request) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "contact not found"));
        contact.setGroupName(request.getGroupName());
        return contactRepository.save(contact);
    }
}
