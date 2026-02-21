package com.tarotalk.common.web;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceContext {
    private TraceContext() {
    }

    public static String currentTraceIdOrRandom() {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }

    public static String currentUserId() {
        String userId = MDC.get("userId");
        return (userId == null || userId.trim().isEmpty()) ? null : userId;
    }
}

