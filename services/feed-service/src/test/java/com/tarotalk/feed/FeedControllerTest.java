package com.tarotalk.feed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FeedControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createFeed() throws Exception {
        String payload = "{\"authorId\":\"00000000-0000-0000-0000-000000000001\",\"content\":\"Hello feed\"}";
        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void v2LikeAndComment() throws Exception {
        String createPayload = "{\"authorId\":\"00000000-0000-0000-0000-000000000001\",\"content\":\"Hello feed\"}";
        MvcResult createResult = mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<?, ?> root = objectMapper.readValue(responseBody, Map.class);
        Map<?, ?> data = (Map<?, ?>) root.get("data");
        String feedId = String.valueOf(data.get("feedId"));

        String likePayload = "{\"userId\":\"00000000-0000-0000-0000-000000000002\",\"action\":\"LIKE\"}";
        mockMvc.perform(post("/api/v2/feeds/" + feedId + "/like")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(likePayload))
                .andExpect(status().isOk());

        String commentPayload = "{\"userId\":\"00000000-0000-0000-0000-000000000003\",\"content\":\"nice\"}";
        mockMvc.perform(post("/api/v2/feeds/" + feedId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPayload))
                .andExpect(status().isOk());
    }
}
