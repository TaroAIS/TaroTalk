package com.tarotalk.chat;

import com.tarotalk.chat.api.ConversationController;
import com.tarotalk.chat.domain.Conversation;
import com.tarotalk.chat.domain.ConversationParticipant;
import com.tarotalk.chat.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConversationController.class)
public class ConversationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversationService conversationService;

    @Test
    void createConversation() throws Exception {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationId, Conversation.Type.ONE_ON_ONE, "Test");
        Mockito.when(conversationService.createConversation(Mockito.any())).thenReturn(conversation);
        Mockito.when(conversationService.getParticipants(Mockito.eq(conversationId)))
                .thenReturn(Collections.singletonList(new ConversationParticipant(UUID.randomUUID(), conversationId, UUID.randomUUID(), ConversationParticipant.Role.OWNER)));

        String payload = "{\"type\":\"ONE_ON_ONE\",\"participantIds\":[\"00000000-0000-0000-0000-000000000001\",\"00000000-0000-0000-0000-000000000002\"]}";
        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
