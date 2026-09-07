package com.example.auth_service.service;

import org.springframework.stereotype.Service;

import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.exception.ExternalServiceException;
import com.example.auth_service.sharedLogic.dto.AuthInfo;
import com.example.auth_service.sharedLogic.dto.AuthResponse;
import com.example.auth_service.sharedLogic.dto.UserDTO;
import com.example.auth_service.sharedLogic.webFlux.UserApiClient;
import reactor.core.publisher.Mono;

/** Auth use cases; HTTP transport belongs to UserApiClient. */
@Service
public class AuthService {
    private final UserApiClient userApiClient;
    private final JwtService jwtService;

    public AuthService(UserApiClient userApiClient, JwtService jwtService) {
        this.userApiClient = userApiClient;
        this.jwtService = jwtService;
    }

    public Mono<AuthInfo> getAuthInfo(String username) {
        return userApiClient.getUserByUsername(username)
                .map(user -> new AuthInfo(user.getId(), user.getUsername(), user.getEmail(),
                        user.getFullName(), user.getRole()));
    }

    public AuthResponse login(String username, String password) {

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("Username and password must not be blank");
        }

        Mono<UserDTO> userMono = userApiClient.getUserByUsername(username);

        UserDTO userInfor = userMono.block();

        if (userInfor == null) {
            throw new UnauthorizedException("Invalid username or password");
        }

        Mono<Boolean> passwordCheckMono = userApiClient.checkPassword(username, password);
        Boolean isPasswordCorrect = passwordCheckMono.block();

        if (isPasswordCorrect == null || !isPasswordCorrect) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = this.jwtService.createToken(AuthInfo.builder()
                .id(userInfor.getId())
                .username(userInfor.getUsername())
                .email(userInfor.getEmail())
                .fullName(userInfor.getFullName())
                .role(userInfor.getRole())
                .build());

        return AuthResponse.builder()
                .token(token)
                .timeLogin(java.time.LocalDateTime.now())
                .build();

    }
}
