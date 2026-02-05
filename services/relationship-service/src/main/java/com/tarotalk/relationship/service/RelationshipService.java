package com.tarotalk.relationship.service;

import com.tarotalk.relationship.api.RelationshipResponse;
import com.tarotalk.relationship.api.RelationshipUpdateRequest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RelationshipService {
    private final Neo4jClient neo4jClient;

    public RelationshipService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public List<RelationshipResponse> getRelationships(String userId) {
        String query = "MATCH (u:User {id: $userId})-[r]->(t) " +
                "RETURN t.id as targetId, type(r) as type, r.intimacyScore as intimacyScore, " +
                "r.interactionCount as interactionCount, r.commercialScore as commercialScore";
        List<RelationshipResponse> responses = new ArrayList<>();
        neo4jClient.query(query).bind(userId).to("userId")
                .fetch().all()
                .forEach(row -> responses.add(mapRow(row)));
        return responses;
    }

    public void upsertRelationship(String userId, RelationshipUpdateRequest request) {
        String query = "MERGE (u:User {id: $userId}) " +
                "MERGE (t:User {id: $targetId}) " +
                "MERGE (u)-[r:" + request.getType() + "]->(t) " +
                "SET r.intimacyScore = $intimacyScore, r.interactionCount = $interactionCount, r.commercialScore = $commercialScore";
        neo4jClient.query(query)
                .bind(userId).to("userId")
                .bind(request.getTargetId()).to("targetId")
                .bind(request.getIntimacyScore()).to("intimacyScore")
                .bind(request.getInteractionCount() == null ? 0L : request.getInteractionCount()).to("interactionCount")
                .bind(request.getCommercialScore() == null ? 0.0 : request.getCommercialScore()).to("commercialScore")
                .run();
    }

    private RelationshipResponse mapRow(Map<String, Object> row) {
        String targetId = String.valueOf(row.get("targetId"));
        String type = String.valueOf(row.get("type"));
        double intimacyScore = row.get("intimacyScore") == null ? 0.0 : ((Number) row.get("intimacyScore")).doubleValue();
        long interactionCount = row.get("interactionCount") == null ? 0L : ((Number) row.get("interactionCount")).longValue();
        double commercialScore = row.get("commercialScore") == null ? 0.0 : ((Number) row.get("commercialScore")).doubleValue();
        return new RelationshipResponse(targetId, type, intimacyScore, interactionCount, commercialScore);
    }
}
