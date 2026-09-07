package com.example.auth_service.sharedLogic.webFlux;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.example.auth_service.exception.ExternalServiceException;
import com.example.auth_service.exception.GatewayTimeoutException;
import com.example.auth_service.exception.UpstreamServiceException;
import com.example.auth_service.exception.ServiceUnavailableException;

import reactor.core.publisher.Mono;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BaseWebFlux {
    private final Duration timeout;

    protected final WebClient webClient;

    public BaseWebFlux(WebClient webClient) {
        this(webClient, Duration.ofSeconds(5));
    }

    public BaseWebFlux(WebClient webClient, Duration timeout) {
        org.springframework.util.Assert.notNull(webClient, "WebClient must not be null");
        org.springframework.util.Assert.isTrue(timeout != null && !timeout.isNegative() && !timeout.isZero(),
                "Timeout must be positive");
        this.webClient = webClient;
        this.timeout = timeout;
    }

    public BaseWebFlux(String baseUrl) {
        this(WebClient.builder().baseUrl(baseUrl).build());
    }

    protected <T> Mono<T> handleResponse(
            WebClient.ResponseSpec responseSpec,
            ParameterizedTypeReference<ResponseFromWebFlux<T>> responseType) {

        return responseSpec
                .onStatus(
                        status -> status.isError(),
                        response -> response
                                .bodyToMono(ResponseFromWebFlux.class)
                                .onErrorResume(org.springframework.core.codec.DecodingException.class,
                                        ex -> Mono.empty())
                                .map(error -> new ExternalServiceException(
                                        response.statusCode().value(),
                                        error.getMessage(),
                                        error.getData()))
                                .defaultIfEmpty(
                                        new ExternalServiceException(
                                                response.statusCode().value(),
                                                "External service returned an error",
                                                null)))
                .bodyToMono(responseType)
                .switchIfEmpty(Mono.error(new UpstreamServiceException("Empty response from upstream service")))
                .flatMap(response -> {

                    if (!Boolean.TRUE.equals(response.getSuccess()) || response.getData() == null) {

                        return Mono.error(
                                new UpstreamServiceException(
                                        "Invalid response from upstream service"));
                    }

                    return Mono.just(response.getData());
                })
                .onErrorMap(
                        org.springframework.core.codec.DecodingException.class,
                        ex -> new UpstreamServiceException(
                                "Invalid response from upstream service"))
                .timeout(timeout)
                .onErrorMap(
                        TimeoutException.class,
                        ex -> new GatewayTimeoutException(
                                "User service timed out"))
                .onErrorMap(
                        WebClientRequestException.class,
                        ex -> new ServiceUnavailableException(
                                "User service is unavailable"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ResponseFromWebFlux<T> {
        private String message;
        private T data;
        private Boolean success;
    }

}
