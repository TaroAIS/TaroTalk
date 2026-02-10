package com.tarotalk.chat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrchestratorReply {
    private String reply;
    private List<Turn> turns = new ArrayList<>();
    private Map<String, String> roleUserMap = new HashMap<>();

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
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

    public static class Turn {
        private int round;
        private String role;
        private String userId;
        private String content;

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
    }
}
