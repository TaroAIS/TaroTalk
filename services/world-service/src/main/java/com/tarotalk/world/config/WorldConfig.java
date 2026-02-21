package com.tarotalk.world.config;

import com.tarotalk.common.http.RestTemplateFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WorldConfig {
    @Bean
    public RestTemplate restTemplate(@Value("${http.client.connect-timeout-ms:3000}") int connectTimeoutMs,
                                     @Value("${http.client.read-timeout-ms:5000}") int readTimeoutMs,
                                     @Value("${http.client.max-attempts:3}") int maxAttempts,
                                     @Value("${http.client.retry-backoff-ms:150}") long retryBackoffMs) {
        return RestTemplateFactory.create(connectTimeoutMs, readTimeoutMs, maxAttempts, retryBackoffMs);
    }
}
