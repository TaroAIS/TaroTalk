package com.tarotalk.persona.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PersonaConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
