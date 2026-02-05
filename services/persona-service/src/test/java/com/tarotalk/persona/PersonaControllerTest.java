package com.tarotalk.persona;

import com.tarotalk.persona.service.AiClient;
import com.tarotalk.persona.service.VectorStoreClient;
import com.tarotalk.persona.service.dto.PersonaGenerationResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PersonaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiClient aiClient;

    @MockBean
    private VectorStoreClient vectorStoreClient;

    @Test
    void createPersona() throws Exception {
        Mockito.when(aiClient.generatePersona(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new PersonaGenerationResult("summary", Collections.emptyMap()));

        String payload = "{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"description\":\"I love art\"}";
        mockMvc.perform(post("/api/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
