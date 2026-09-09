package com.example.auth_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.sharedLogic.dto.LoginResult;
import com.example.auth_service.sharedLogic.dto.AuthInfo;
import com.example.auth_service.sharedLogic.dto.UserDTO;
import com.example.auth_service.sharedLogic.dto.request.UserRegister;
import com.example.auth_service.exception.ExternalServiceException;
import com.example.auth_service.exception.UpstreamServiceException;
import com.example.auth_service.sharedLogic.webFlux.UserApiClient;
import reactor.core.publisher.Mono;

class AuthServiceTests {
    private final UserApiClient client = mock(UserApiClient.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuthService service = new AuthService(client, jwt);

    @Test
    void registrationIssuesTokenUsingCreatedUser() {
        var request = new UserRegister("alice", "alice@example.com", "correct", null);
        UserDTO user = new UserDTO();
        user.setId(42L);
        user.setUsername("alice");
        user.setRole("USER");
        when(client.register(request)).thenReturn(Mono.just(new LoginResult(user, true)));
        when(jwt.createToken(any(AuthInfo.class))).thenReturn("new-token");
        var response = service.register(request);
        assertEquals("new-token", response.getToken());
        assertNotNull(response.getTimeLogin());
        verify(jwt).createToken(argThat(info -> Long.valueOf(42).equals(info.getId())
                && "USER".equals(info.getRole())));
        verify(client, never()).isLogin(anyString(), anyString());
    }

    @Test
    void registrationPreservesDuplicateAccountError() {
        var request = new UserRegister("alice", "alice@example.com", "correct", null);
        var error = new ExternalServiceException(400, "Username already exists", null);
        when(client.register(request)).thenReturn(Mono.error(error));
        assertSame(error, assertThrows(ExternalServiceException.class, () -> service.register(request)));
        verifyNoInteractions(jwt);
    }

    @Test
    void registrationRejectsMissingUserWithoutIssuingToken() {
        var request = new UserRegister("alice", "alice@example.com", "correct", null);
        when(client.register(request)).thenReturn(Mono.just(new LoginResult(null, true)));
        assertThrows(UpstreamServiceException.class, () -> service.register(request));
        verifyNoInteractions(jwt);
    }

    @Test
    void issuesTokenAfterAccountSpecificPasswordCheck() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole("USER");

        when(client.isLogin("alice", "correct")).thenReturn(Mono.just(new LoginResult(user, true)));
        when(jwt.createToken(any(AuthInfo.class))).thenReturn("signed-token");
        var response = service.login("alice", "correct");
        assertEquals("signed-token", response.getToken());
        assertNotNull(response.getTimeLogin());
        verify(jwt).createToken(argThat(info -> "alice".equals(info.getUsername()) && "USER".equals(info.getRole())));
    }

    @Test
    void rejectsWrongPasswordWithoutIssuingToken() {

        when(client.isLogin("alice", "wrong")).thenReturn(Mono.just(new LoginResult(null, false)));
        assertThrows(UnauthorizedException.class, () -> service.login("alice", "wrong"));
        verifyNoInteractions(jwt);
    }

    @Test
    void mapsMissingAccountToUnauthorized() {
        when(client.isLogin("missing", "wrong"))
                .thenReturn(Mono.just(new LoginResult(null, false)));
        assertThrows(UnauthorizedException.class, () -> service.login("missing", "wrong"));
        verifyNoInteractions(jwt);
    }
}
