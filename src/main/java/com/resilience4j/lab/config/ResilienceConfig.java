package com.resilience4j.lab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ResilienceConfig {

    // Centralized RestTemplate bean — ready for customization (timeouts, interceptors)
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}