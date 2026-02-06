package com.tarotalk.feed.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class FeedConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
