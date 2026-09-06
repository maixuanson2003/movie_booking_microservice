package com.example.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebFluxConfig {
    @Bean
    public WebClient webClient(WebClient.Builder builder,
            @Value("${clients.user.base-url:http://localhost:8081}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
