package com.example.email_service.sharedLogic.webFlux;

import org.springframework.web.reactive.function.client.WebClient;

import com.example.email_service.exception.ExternalServiceException;

import reactor.core.publisher.Mono;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BaseWebFlux {

    protected final WebClient webClient;

    public BaseWebFlux(String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    protected <T> Mono<T> handleResponse(
            WebClient.ResponseSpec responseSpec,
            Class<T> responseType) {

        return responseSpec
                .onStatus(
                        status -> status.isError(),
                        response -> response
                                .bodyToMono(ResponseFromWebFlux.class)
                                .onErrorResume(org.springframework.core.codec.DecodingException.class, ex -> Mono.empty())
                                .map(error -> new ExternalServiceException(
                                        response.statusCode().value(),
                                        error.getMessage(),
                                        error.getData()))
                                .defaultIfEmpty(new ExternalServiceException(
                                        response.statusCode().value(),
                                        "External service returned an error", null)))
                .bodyToMono(responseType);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ResponseFromWebFlux {
        private String message;
        private Object data;
    }

}
