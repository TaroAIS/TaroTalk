package com.tarotalk.world.api;

import java.util.ArrayList;
import java.util.List;

public class GoalEconomyEvaluateRequest {
    private List<String> actorIds = new ArrayList<>();
    private String objective;

    public List<String> getActorIds() {
        return actorIds;
    }

    public void setActorIds(List<String> actorIds) {
        this.actorIds = actorIds;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }
}
