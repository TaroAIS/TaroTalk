package com.tarotalk.user.service;

import com.tarotalk.common.api.PageResponse;
import com.tarotalk.common.exception.ApiException;
import com.tarotalk.user.api.ContactGroupRequest;
import com.tarotalk.user.api.CreateContactRequest;
import com.tarotalk.user.domain.Contact;
import com.tarotalk.user.domain.UserProfile;
import com.tarotalk.user.domain.UserType;
import com.tarotalk.user.repo.ContactRepository;
import com.tarotalk.user.repo.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
import java.util.List;

@Service
public class ContactService {
    private final ContactRepository contactRepository;
    private final UserProfileRepository userProfileRepository;

    public ContactService(ContactRepository contactRepository, UserProfileRepository userProfileRepository) {
        this.contactRepository = contactRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public PageResponse<Contact> listContacts(UUID userId, int page, int size) {
        Page<Contact> result = contactRepository.findByUserId(userId, PageRequest.of(page, size));
        return filterAiContacts(result, page, size);
    }

    public PageResponse<Contact> searchContacts(UUID userId, String keyword, int page, int size) {
        Page<Contact> result = contactRepository.findByUserIdAndGroupNameContainingIgnoreCase(userId, keyword, PageRequest.of(page, size));
        return filterAiContacts(result, page, size);
    }

    public Contact updateGroup(UUID contactId, ContactGroupRequest request) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "contact not found"));
        contact.setGroupName(request.getGroupName());
        return contactRepository.save(contact);
    }

    public Contact createContact(CreateContactRequest request) {
        UserProfile contactProfile = userProfileRepository.findById(request.getContactUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "contact user not found"));
        if (contactProfile.getUserType() == UserType.HUMAN) {
            throw new ApiException("VALIDATION_ERROR", "contact must be AI user");
        }
        Contact contact = new Contact(UUID.randomUUID(), request.getUserId(), request.getContactUserId());
        contact.setGroupName(request.getGroupName());
        contact.setBlocked(Boolean.TRUE.equals(request.getBlocked()));
        return contactRepository.save(contact);
    }

    public List<UUID> listContactUserIds(UUID userId) {
        List<Contact> contacts = contactRepository.findByUserIdAndBlockedFalse(userId);
        if (contacts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<UUID, UserProfile> profiles = new HashMap<>();
        List<UUID> contactUserIds = contacts.stream()
                .map(Contact::getContactUserId)
                .collect(java.util.stream.Collectors.toList());
        userProfileRepository.findAllById(contactUserIds)
                .forEach(profile -> profiles.put(profile.getUserId(), profile));
        List<UUID> filtered = new java.util.ArrayList<>();
        for (Contact contact : contacts) {
            UserProfile profile = profiles.get(contact.getContactUserId());
            if (profile != null && profile.getUserType() != UserType.HUMAN) {
                filtered.add(contact.getContactUserId());
            }
        }
        return filtered;
    }

    public List<UUID> listOwnerIdsByContactUserId(UUID contactUserId) {
        List<Contact> contacts = contactRepository.findByContactUserIdAndBlockedFalse(contactUserId);
        if (contacts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<UUID, UserProfile> profiles = new HashMap<>();
        List<UUID> ownerIds = contacts.stream()
                .map(Contact::getUserId)
                .collect(java.util.stream.Collectors.toList());
        userProfileRepository.findAllById(ownerIds)
                .forEach(profile -> profiles.put(profile.getUserId(), profile));
        List<UUID> filtered = new java.util.ArrayList<>();
        for (Contact contact : contacts) {
            UserProfile profile = profiles.get(contact.getUserId());
            if (profile != null && profile.getUserType() != UserType.HUMAN) {
                filtered.add(contact.getUserId());
            }
        }
        return filtered;
    }

    private PageResponse<Contact> filterAiContacts(Page<Contact> result, int page, int size) {
        Map<UUID, UserProfile> profiles = new HashMap<>();
        java.util.List<UUID> contactUserIds = result.getContent().stream()
                .map(Contact::getContactUserId)
                .collect(java.util.stream.Collectors.toList());
        userProfileRepository.findAllById(contactUserIds)
                .forEach(profile -> profiles.put(profile.getUserId(), profile));
        java.util.List<Contact> filtered = new java.util.ArrayList<>();
        for (Contact contact : result.getContent()) {
            UserProfile profile = profiles.get(contact.getContactUserId());
            if (profile != null && profile.getUserType() != UserType.HUMAN) {
                filtered.add(contact);
            }
        }
        return new PageResponse<>(filtered, page, size, filtered.size());
    }
}
