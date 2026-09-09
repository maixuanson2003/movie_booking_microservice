package com.example.auth_service.sharedLogic.webFlux;

import java.time.Duration;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.auth_service.sharedLogic.dto.UserDTO;
import com.example.auth_service.sharedLogic.dto.LoginResult;
import com.example.auth_service.sharedLogic.dto.request.UserRegister;
import com.example.auth_service.exception.BadRequestException;
import reactor.core.publisher.Mono;

@Component
public class UserApiClient extends BaseWebFlux {
    private static final ParameterizedTypeReference<ResponseFromWebFlux<UserDTO>> USER_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<ResponseFromWebFlux<LoginResult>> LOGIN_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    public UserApiClient(WebClient webClient, @Value("${clients.user.timeout:5s}") Duration timeout) {
        super(webClient, timeout);
    }

    // public Mono<UserDTO> getUser(Long id) {
    // if (id == null || id <= 0)
    // return Mono.error(new BadRequestException("User id must be positive"));
    // return readUser(webClient.get().uri("/api/users/{id}", Map.of("id",
    // id)).retrieve());
    // }

    /** Lookup contract provided by user-service. */
    public Mono<UserDTO> getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Mono.error(new BadRequestException("Username must not be blank"));
        }
        return handleResponse(
                webClient.get().uri("/api/users/username/{username}", Map.of("username", username)).retrieve(),
                USER_RESPONSE_TYPE)
                .map(user -> {
                    user.setPassword(null);
                    return user;
                });
    }

    public Mono<LoginResult> register(UserRegister request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return Mono.error(new BadRequestException("Username, email and password must not be blank"));
        }
        return handleResponse(webClient.post().uri("/api/users/register")
                .bodyValue(request).retrieve(), LOGIN_RESPONSE_TYPE)
                .map(result -> {
                    if (result.userDto() != null) {
                        result.userDto().setPassword(null);
                    }
                    return result;
                });
    }

    public Mono<LoginResult> isLogin(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Mono.error(new BadRequestException("Username and password must not be blank"));
        }
        return handleResponse(
                webClient.post().uri("/api/users/isLogin")
                        .bodyValue(Map.of("username", username, "password", password)).retrieve(),
                LOGIN_RESPONSE_TYPE)
                .map(result -> {
                    if (result.userDto() != null) {
                        result.userDto().setPassword(null);
                    }
                    return result;
                });
    }

}
