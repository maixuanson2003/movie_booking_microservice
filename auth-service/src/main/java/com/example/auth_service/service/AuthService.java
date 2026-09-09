package com.example.auth_service.service;

import org.springframework.stereotype.Service;

import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.exception.UpstreamServiceException;
import com.example.auth_service.sharedLogic.dto.LoginResult;
import com.example.auth_service.sharedLogic.dto.AuthInfo;
import com.example.auth_service.sharedLogic.dto.AuthResponse;
import com.example.auth_service.sharedLogic.dto.UserDTO;
import com.example.auth_service.sharedLogic.dto.request.UserRegister;
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

    public AuthResponse register(UserRegister request) {
        LoginResult result = userApiClient.register(request).block();
        if (result == null || !result.isLogin() || result.userDto() == null) {
            throw new UpstreamServiceException("Invalid registration response from user service");
        }
        return createAuthResponse(result.userDto());
    }

    public AuthResponse login(String username, String password) {

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("Username and password must not be blank");
        }

        LoginResult result = userApiClient.isLogin(username, password).block();

        if (result == null || !result.isLogin()) {
            throw new UnauthorizedException("Invalid username or password");
        }

        UserDTO userInfor = result.userDto();
        if (userInfor == null) {
            throw new UpstreamServiceException("Login response is missing userDto");
        }

        return createAuthResponse(userInfor);
    }

    private AuthResponse createAuthResponse(UserDTO userInfor) {
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
