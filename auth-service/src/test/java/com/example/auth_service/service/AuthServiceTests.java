package com.example.auth_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.exception.ExternalServiceException;
import com.example.auth_service.sharedLogic.dto.AuthInfo;
import com.example.auth_service.sharedLogic.dto.UserDTO;
import com.example.auth_service.sharedLogic.webFlux.UserApiClient;
import reactor.core.publisher.Mono;

class AuthServiceTests {
    private final UserApiClient client = mock(UserApiClient.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuthService service = new AuthService(client, jwt);

    @Test
    void issuesTokenAfterAccountSpecificPasswordCheck() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole("USER");
        when(client.getUserByUsername("alice")).thenReturn(Mono.just(user));
        when(client.checkPassword("alice", "correct")).thenReturn(Mono.just(true));
        when(jwt.createToken(any(AuthInfo.class))).thenReturn("signed-token");
        var response = service.login("alice", "correct");
        assertEquals("signed-token", response.getToken());
        assertNotNull(response.getTimeLogin());
        verify(jwt).createToken(argThat(info -> "alice".equals(info.getUsername()) && "USER".equals(info.getRole())));
    }

    @Test
    void rejectsWrongPasswordWithoutIssuingToken() {
        when(client.getUserByUsername("alice")).thenReturn(Mono.just(new UserDTO()));
        when(client.checkPassword("alice", "wrong")).thenReturn(Mono.just(false));
        assertThrows(UnauthorizedException.class, () -> service.login("alice", "wrong"));
        verifyNoInteractions(jwt);
    }

    @Test
    void mapsMissingAccountToUnauthorized() {
        when(client.getUserByUsername("missing"))
                .thenReturn(Mono.error(new ExternalServiceException(404, "User not found", null)));
        assertThrows(UnauthorizedException.class, () -> service.login("missing", "wrong"));
        verifyNoInteractions(jwt);
    }
}
