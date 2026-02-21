package com.tarotalk.feed.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Service
public class BanditPolicyService {
    public double scoreShadow(UUID viewerId, UUID feedId, Map<String, Double> features) {
        double freshness = safe(features.get("freshness"));
        double interactionVelocity = safe(features.get("interactionVelocity"));
        double relationshipAffinity = safe(features.get("relationshipAffinity"));
        double narrativeRelevance = safe(features.get("narrativeRelevance"));

        // Lightweight LinUCB-like linear utility for shadow scoring.
        double linearUtility = freshness * 0.30
                + interactionVelocity * 0.25
                + relationshipAffinity * 0.25
                + narrativeRelevance * 0.20;

        // Small epsilon-style exploration term, deterministic by (viewer, feed).
        double exploration = deterministicNoise(viewerId, feedId) * 0.08;
        return clamp01(linearUtility + exploration);
    }

    private double deterministicNoise(UUID viewerId, UUID feedId) {
        try {
            String seed = String.valueOf(viewerId) + ":" + String.valueOf(feedId);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(seed.getBytes(StandardCharsets.UTF_8));
            int value = ((digest[0] & 0xFF) << 24)
                    | ((digest[1] & 0xFF) << 16)
                    | ((digest[2] & 0xFF) << 8)
                    | (digest[3] & 0xFF);
            long normalized = value & 0xFFFFFFFFL;
            return normalized / (double) 0xFFFFFFFFL;
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
