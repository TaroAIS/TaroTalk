package com.tarotalk.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
            "/api/auth/",
            "/actuator/",
            "/api/a2a/tools"
    );

    private static final List<String> PROTECTED_PREFIXES = Arrays.asList(
            "/api/v2/",
            "/api/conversations/",
            "/api/feeds/",
            "/api/relationships/",
            "/api/scheduler/"
    );

    @Value("${security.jwt.secret:dev-secret-change-me-dev-secret-change-me}")
    private String secret;

    @Value("${security.jwt.enforce:false}")
    private boolean enforce;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = resolveBearerToken(exchange.getRequest().getHeaders());
        if (token == null) {
            if (enforce && isProtectedPath(path)) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "missing bearer token");
            }
            return chain.filter(exchange);
        }

        Claims claims;
        try {
            claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (Exception ex) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "invalid bearer token");
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        if (role == null || role.trim().isEmpty()) {
            role = "USER";
        }
        final String finalRole = role;

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    if (userId != null && !userId.trim().isEmpty()) {
                        headers.set("X-User-Id", userId);
                    }
                    headers.set("X-Role", finalRole);
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProtectedPath(String path) {
        for (String prefix : PROTECTED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String resolveBearerToken(HttpHeaders headers) {
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.trim().isEmpty()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!auth.startsWith(prefix)) {
            return null;
        }
        String token = auth.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().set("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
