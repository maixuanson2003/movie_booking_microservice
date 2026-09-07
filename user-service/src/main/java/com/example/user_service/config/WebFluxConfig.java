package com.example.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebFluxConfig {

    private final String baseUrl = "http://movie-service:8080";

    @Bean
    public WebClient webClient(ObjectProvider<WebClient.Builder> builders) {
        WebClient.Builder builder = builders.getIfAvailable(WebClient::builder);
        return builder
                .baseUrl(baseUrl)
                .build();
    }

}
