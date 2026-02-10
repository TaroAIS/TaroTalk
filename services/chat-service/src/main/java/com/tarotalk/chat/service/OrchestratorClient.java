package com.tarotalk.chat.service;

import com.tarotalk.chat.api.MessageContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrchestratorClient {
    private final RestTemplate restTemplate;
    private final String orchestratorUrl;

    public OrchestratorClient(RestTemplate restTemplate,
                              @Value("${integrations.orchestrator.base-url:}") String orchestratorUrl) {
        this.restTemplate = restTemplate;
        this.orchestratorUrl = orchestratorUrl;
    }

    public OrchestratorReply generateReply(String conversationId,
                                           String personaSummary,
                                           List<MessageContext> context,
                                           List<java.util.UUID> participants,
                                           java.util.UUID senderId) {
        if (orchestratorUrl == null || orchestratorUrl.trim().isEmpty()) {
            return new OrchestratorReply();
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversation_id", conversationId);
        payload.put("participants", participants);
        payload.put("sender_id", senderId == null ? null : senderId.toString());
        payload.put("persona_summary", personaSummary);
        payload.put("messages", context);
        Map response = restTemplate.postForObject(orchestratorUrl + "/a2a/chat", payload, Map.class);
        if (response == null) {
            return new OrchestratorReply();
        }
        Map<String, Object> body = response;
        if (response.get("data") instanceof Map) {
            body = (Map<String, Object>) response.get("data");
        }

        OrchestratorReply orchestratorReply = new OrchestratorReply();
        Object reply = body.get("reply");
        orchestratorReply.setReply(reply == null ? "" : String.valueOf(reply));

        Object turns = body.get("turns");
        if (turns instanceof List) {
            List<OrchestratorReply.Turn> parsedTurns = new ArrayList<>();
            for (Object item : (List<?>) turns) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> row = (Map<?, ?>) item;
                OrchestratorReply.Turn turn = new OrchestratorReply.Turn();
                Object round = row.get("round");
                if (round instanceof Number) {
                    turn.setRound(((Number) round).intValue());
                }
                Object role = row.containsKey("role") ? row.get("role") : "";
                turn.setRole(String.valueOf(role));
                Object userId = row.containsKey("user_id") ? row.get("user_id") : row.get("userId");
                turn.setUserId(String.valueOf(userId == null ? "" : userId));
                Object content = row.containsKey("content") ? row.get("content") : "";
                turn.setContent(String.valueOf(content));
                if (!turn.getContent().isEmpty()) {
                    parsedTurns.add(turn);
                }
            }
            orchestratorReply.setTurns(parsedTurns);
        }

        Object roleUserMap = body.get("role_user_map");
        if (roleUserMap instanceof Map) {
            Map<String, String> parsed = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) roleUserMap).entrySet()) {
                parsed.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            orchestratorReply.setRoleUserMap(parsed);
        }

        return orchestratorReply;
    }
}
