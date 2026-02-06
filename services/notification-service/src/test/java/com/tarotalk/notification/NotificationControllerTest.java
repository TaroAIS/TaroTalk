package com.tarotalk.notification;

import com.tarotalk.notification.domain.Notification;
import com.tarotalk.notification.repo.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void createNotification() throws Exception {
        String payload = "{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"type\":\"SYSTEM\",\"title\":\"Hello\",\"content\":\"Welcome\"}";
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void listNotificationsWithFilters() throws Exception {
        UUID userId = UUID.randomUUID();
        Notification feedCreated = new Notification(UUID.randomUUID(), userId, Notification.Type.FEED_CREATED, "New Feed", "feed");
        Notification system = new Notification(UUID.randomUUID(), userId, Notification.Type.SYSTEM, "Sys", "sys");
        notificationRepository.save(feedCreated);
        notificationRepository.save(system);

        mockMvc.perform(get("/api/notifications")
                        .param("userId", userId.toString())
                        .param("types", "FEED_CREATED")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("FEED_CREATED"));
    }
}
