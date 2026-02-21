package com.tarotalk.chat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrchestratorReply {
    private String reply;
    private String traceId;
    private List<Map<String, Object>> toolCalls = new ArrayList<>();
    private List<Turn> turns = new ArrayList<>();
    private Map<String, String> roleUserMap = new HashMap<>();
    private Map<String, Object> directorTrace = new HashMap<>();
    private List<Map<String, Object>> stateEffects = new ArrayList<>();

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<Map<String, Object>> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<Map<String, Object>> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<Turn> getTurns() {
        return turns;
    }

    public void setTurns(List<Turn> turns) {
        this.turns = turns;
    }

    public Map<String, String> getRoleUserMap() {
        return roleUserMap;
    }

    public void setRoleUserMap(Map<String, String> roleUserMap) {
        this.roleUserMap = roleUserMap;
    }

    public Map<String, Object> getDirectorTrace() {
        return directorTrace;
    }

    public void setDirectorTrace(Map<String, Object> directorTrace) {
        this.directorTrace = directorTrace;
    }

    public List<Map<String, Object>> getStateEffects() {
        return stateEffects;
    }

    public void setStateEffects(List<Map<String, Object>> stateEffects) {
        this.stateEffects = stateEffects;
    }

    public static class Turn {
        private int round;
        private String role;
        private String userId;
        private String content;
        private String effectRef;

        public int getRound() {
            return round;
        }

        public void setRound(int round) {
            this.round = round;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getEffectRef() {
            return effectRef;
        }

        public void setEffectRef(String effectRef) {
            this.effectRef = effectRef;
        }
    }
}
