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
                                           java.util.UUID senderId,
                                           String worldId,
                                           Integer contextWindow,
                                           String intent) {
        if (orchestratorUrl == null || orchestratorUrl.trim().isEmpty()) {
            return new OrchestratorReply();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("conversation_id", conversationId);
        payload.put("participants", participants);
        payload.put("sender_id", senderId == null ? null : senderId.toString());
        payload.put("persona_summary", personaSummary);
        payload.put("messages", context);
        payload.put("world_id", worldId);
        payload.put("context_window", contextWindow);
        payload.put("intent", intent);

        Map response = postChat(payload);
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
        Object traceId = body.get("trace_id");
        if (traceId != null) {
            orchestratorReply.setTraceId(String.valueOf(traceId));
        }

        Object toolCalls = body.get("tool_calls");
        if (toolCalls instanceof List) {
            List<Map<String, Object>> parsedToolCalls = new ArrayList<>();
            for (Object item : (List<?>) toolCalls) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                parsedToolCalls.add(row);
            }
            orchestratorReply.setToolCalls(parsedToolCalls);
        }

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
                turn.setRole(String.valueOf(row.containsKey("role") ? row.get("role") : ""));
                Object userId = row.containsKey("user_id") ? row.get("user_id") : row.get("userId");
                turn.setUserId(String.valueOf(userId == null ? "" : userId));
                turn.setContent(String.valueOf(row.containsKey("content") ? row.get("content") : ""));
                Object effectRef = row.containsKey("effect_ref") ? row.get("effect_ref") : row.get("effectRef");
                if (effectRef != null) {
                    turn.setEffectRef(String.valueOf(effectRef));
                }
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

        Object directorTrace = body.get("director_trace");
        if (directorTrace instanceof Map) {
            Map<String, Object> parsed = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) directorTrace).entrySet()) {
                parsed.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            orchestratorReply.setDirectorTrace(parsed);
        }

        Object stateEffects = body.get("state_effects");
        if (stateEffects instanceof List) {
            List<Map<String, Object>> parsed = new ArrayList<>();
            for (Object item : (List<?>) stateEffects) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                parsed.add(row);
            }
            orchestratorReply.setStateEffects(parsed);
        }

        return orchestratorReply;
    }

    @SuppressWarnings("unchecked")
    private Map postChat(Map<String, Object> payload) {
        try {
            return restTemplate.postForObject(orchestratorUrl + "/api/v2/a2a/chat", payload, Map.class);
        } catch (Exception ex) {
            try {
                return restTemplate.postForObject(orchestratorUrl + "/a2a/chat", payload, Map.class);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
