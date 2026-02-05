package com.tarotalk.common.web;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String REQUEST_HEADER = "X-Request-Id";
    public static final String USER_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = getFirstNonEmpty(request.getHeader(TRACE_HEADER), request.getHeader(REQUEST_HEADER));
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        String userId = request.getHeader(USER_HEADER);
        MDC.put("traceId", traceId);
        if (userId != null) {
            MDC.put("userId", userId);
        }
        response.setHeader(TRACE_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("userId");
        }
    }

    private String getFirstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }
}
