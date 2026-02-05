package com.tarotalk.presence.service;

import com.tarotalk.presence.api.HeartbeatRequest;
import com.tarotalk.presence.api.PresenceResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PresenceService {
    private static final String KEY_PREFIX = "presence:";

    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public PresenceResponse heartbeat(HeartbeatRequest request) {
        String key = KEY_PREFIX + request.getUserId();
        redisTemplate.opsForValue().set(key, request.getStatus() + "|" + Instant.now().toString(), 5, TimeUnit.MINUTES);
        return getPresence(request.getUserId());
    }

    public PresenceResponse getPresence(UUID userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (value == null) {
            return new PresenceResponse(userId, "OFFLINE", null);
        }
        String[] parts = value.split("\\|");
        String status = parts.length > 0 ? parts[0] : "OFFLINE";
        Instant lastSeen = parts.length > 1 ? Instant.parse(parts[1]) : null;
        return new PresenceResponse(userId, status, lastSeen);
    }
}
