package com.example.auth_service.sharedLogic.webFlux;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import com.example.auth_service.sharedLogic.dto.UserDTO;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.UpstreamServiceException;
import com.example.auth_service.exception.GatewayTimeoutException;
import com.example.auth_service.exception.ServiceUnavailableException;
import reactor.core.publisher.Mono;

@Component
public class UserApiClient extends BaseWebFlux {
    private final Duration timeout;

    public UserApiClient(WebClient webClient, @Value("${clients.user.timeout:5s}") Duration timeout) {
        super(webClient);
        this.timeout = timeout;
    }

    public Mono<UserDTO> getUser(Long id) {
        if (id == null || id <= 0) return Mono.error(new BadRequestException("User id must be positive"));
        return handleResponse(webClient.get().uri("/api/users/{id}", id).retrieve(), UserResponse.class)
                .switchIfEmpty(Mono.error(new UpstreamServiceException("Empty response from user service")))
                .flatMap(response -> {
                    if (!Boolean.TRUE.equals(response.success()) || response.data() == null) {
                        return Mono.error(new UpstreamServiceException("Invalid response from user service"));
                    }
                    response.data().setPassword(null);
                    return Mono.just(response.data());
                })
                .timeout(timeout)
                .onErrorMap(TimeoutException.class,
                        ex -> new GatewayTimeoutException("User service timed out"))
                .onErrorMap(WebClientRequestException.class,
                        ex -> new ServiceUnavailableException("User service is unavailable"));
    }

    public record UserResponse(String message, UserDTO data, Boolean success) { }
}
