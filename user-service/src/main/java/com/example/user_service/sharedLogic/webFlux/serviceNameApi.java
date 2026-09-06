package com.example.user_service.sharedLogic.webFlux;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class serviceNameApi extends BaseWebFlux {

    public serviceNameApi() {
        super("http://service-name:8080");

    }

    public Mono<Object> getUser(Long id) {
        return this.handleResponse(this.webClient
                .get()
                .uri("/users/{id}", id)
                .retrieve(), Object.class);
    }

}
