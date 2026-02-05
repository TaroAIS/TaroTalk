package com.tarotalk.user.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.common.api.PageResponse;
import com.tarotalk.user.domain.Contact;
import com.tarotalk.user.service.ContactService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ApiResponse<ContactResponse> create(@Valid @RequestBody CreateContactRequest request) {
        return ApiResponse.ok(ContactResponse.from(contactService.createContact(request)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ContactResponse>> list(@RequestParam UUID userId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        PageResponse<Contact> contacts = contactService.listContacts(userId, page, size);
        PageResponse<ContactResponse> response = new PageResponse<>();
        response.setItems(contacts.getItems().stream().map(ContactResponse::from).collect(java.util.stream.Collectors.toList()));
        response.setPage(page);
        response.setSize(size);
        response.setTotal(contacts.getTotal());
        return ApiResponse.ok(response);
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ContactResponse>> search(@RequestParam UUID userId,
                                                             @RequestParam String keyword,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        PageResponse<Contact> contacts = contactService.searchContacts(userId, keyword, page, size);
        PageResponse<ContactResponse> response = new PageResponse<>();
        response.setItems(contacts.getItems().stream().map(ContactResponse::from).collect(java.util.stream.Collectors.toList()));
        response.setPage(page);
        response.setSize(size);
        response.setTotal(contacts.getTotal());
        return ApiResponse.ok(response);
    }

    @PutMapping("/{contactId}/group")
    public ApiResponse<ContactResponse> updateGroup(@PathVariable UUID contactId,
                                                    @Valid @RequestBody ContactGroupRequest request) {
        return ApiResponse.ok(ContactResponse.from(contactService.updateGroup(contactId, request)));
    }
}
