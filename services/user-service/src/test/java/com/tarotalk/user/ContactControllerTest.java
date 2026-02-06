package com.tarotalk.user;

import com.tarotalk.user.domain.Contact;
import com.tarotalk.user.domain.UserProfile;
import com.tarotalk.user.domain.UserType;
import com.tarotalk.user.repo.ContactRepository;
import com.tarotalk.user.repo.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ContactControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void listContactIdsFiltersBlockedAndHuman() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID aiId = UUID.randomUUID();
        UUID blockedAiId = UUID.randomUUID();
        UUID humanId = UUID.randomUUID();

        UserProfile aiProfile = new UserProfile(aiId, "AI-1", null);
        aiProfile.setUserType(UserType.AI);
        userProfileRepository.save(aiProfile);

        UserProfile blockedAiProfile = new UserProfile(blockedAiId, "AI-2", null);
        blockedAiProfile.setUserType(UserType.AI);
        userProfileRepository.save(blockedAiProfile);

        UserProfile humanProfile = new UserProfile(humanId, "Human", null);
        humanProfile.setUserType(UserType.HUMAN);
        userProfileRepository.save(humanProfile);

        Contact visibleAi = new Contact(UUID.randomUUID(), ownerId, aiId);
        visibleAi.setBlocked(false);
        contactRepository.save(visibleAi);

        Contact blockedAi = new Contact(UUID.randomUUID(), ownerId, blockedAiId);
        blockedAi.setBlocked(true);
        contactRepository.save(blockedAi);

        Contact humanContact = new Contact(UUID.randomUUID(), ownerId, humanId);
        humanContact.setBlocked(false);
        contactRepository.save(humanContact);

        mockMvc.perform(get("/api/contacts/ids")
                        .param("userId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value(aiId.toString()));
    }

    @Test
    void listOwnersFiltersBlockedAndHuman() throws Exception {
        UUID viewerId = UUID.randomUUID();
        UUID aiOwnerId = UUID.randomUUID();
        UUID humanOwnerId = UUID.randomUUID();
        UUID blockedOwnerId = UUID.randomUUID();

        UserProfile aiOwner = new UserProfile(aiOwnerId, "AI-Owner", null);
        aiOwner.setUserType(UserType.AI);
        userProfileRepository.save(aiOwner);

        UserProfile humanOwner = new UserProfile(humanOwnerId, "Human-Owner", null);
        humanOwner.setUserType(UserType.HUMAN);
        userProfileRepository.save(humanOwner);

        UserProfile blockedOwner = new UserProfile(blockedOwnerId, "Blocked-Owner", null);
        blockedOwner.setUserType(UserType.AI);
        userProfileRepository.save(blockedOwner);

        Contact visible = new Contact(UUID.randomUUID(), aiOwnerId, viewerId);
        visible.setBlocked(false);
        contactRepository.save(visible);

        Contact human = new Contact(UUID.randomUUID(), humanOwnerId, viewerId);
        human.setBlocked(false);
        contactRepository.save(human);

        Contact blocked = new Contact(UUID.randomUUID(), blockedOwnerId, viewerId);
        blocked.setBlocked(true);
        contactRepository.save(blocked);

        mockMvc.perform(get("/api/contacts/owners")
                        .param("contactUserId", viewerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value(aiOwnerId.toString()));
    }
}
