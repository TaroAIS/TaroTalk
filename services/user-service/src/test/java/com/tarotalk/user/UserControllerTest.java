package com.tarotalk.user;

import com.tarotalk.user.domain.UserProfile;
import com.tarotalk.user.repo.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void getUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(userId, "Tester", null));
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(new UserProfile(userId, "Tester", null));
        String payload = "{\"nickname\":\"Updated\"}";
        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
