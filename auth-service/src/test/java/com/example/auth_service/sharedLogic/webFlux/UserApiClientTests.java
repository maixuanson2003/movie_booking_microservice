package com.example.auth_service.sharedLogic.webFlux;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.auth_service.exception.ExternalServiceException;
import com.example.auth_service.exception.GatewayTimeoutException;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.UpstreamServiceException;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class UserApiClientTests {
    @Test
    void registrationPostsAndDecodesLoginResult() {
        var api = new UserApiClient(WebClient.builder().exchangeFunction(request -> {
            assertEquals(org.springframework.http.HttpMethod.POST, request.method());
            assertEquals("/api/users/register", request.url().getPath());
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                    .body("{\"success\":true,\"data\":{\"isLogin\":true,\"userDto\":{\"id\":42,\"username\":\"alice\"}}}")
                    .build());
        }).build(), Duration.ofSeconds(1));
        var result = api.register(new com.example.auth_service.sharedLogic.dto.request.UserRegister(
                "alice", "alice@example.com", "correct", null)).block();
        assertNotNull(result);
        assertTrue(result.isLogin());
        assertEquals(42L, result.userDto().getId());
    }

    private UserApiClient client(HttpStatus status, String body) {
        return new UserApiClient(WebClient.builder().exchangeFunction(request -> {
            assertEquals("/api/users/username/alice", request.url().getPath());
            return Mono.just(ClientResponse.create(status).header("Content-Type", "application/json")
                    .body(body).build());
        }).build(), Duration.ofSeconds(1));
    }

    @Test
    void loginDecodesUserAndLoginFlag() {
        var api = new UserApiClient(WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                        .body("{\"success\":true,\"data\":{\"isLogin\":true,\"userDto\":{\"id\":1,\"username\":\"alice\",\"password\":\"secret\"}}}")
                        .build())).build(), Duration.ofSeconds(1));
        var result = api.isLogin("alice", "correct").block();
        assertNotNull(result);
        assertTrue(result.isLogin());
        assertEquals("alice", result.userDto().getUsername());
        assertNull(result.userDto().getPassword());
    }

    @Test
    void decodesUserAndUnwrapsEnvelope() {
        var user = client(HttpStatus.OK,
                "{\"message\":\"OK\",\"success\":true,\"data\":{\"id\":1,\"password\":\"secret\"}}")
                .getUserByUsername("alice").block();
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertNull(user.getPassword());
    }

    @Test
    void delegatesRemoteErrorsToBaseWebFluxIncludingEmptyBody() {
        for (String body : new String[] {"", "{\"message\":\"Missing\",\"data\":null,\"success\":false}"}) {
            var error = assertThrows(ExternalServiceException.class,
                    () -> client(HttpStatus.NOT_FOUND, body).getUserByUsername("alice").block());
            assertEquals(404, error.getStatus());
        }
    }

    @Test
    void appliesTimeout() {
        var api = new UserApiClient(WebClient.builder().exchangeFunction(request -> Mono.never()).build(),
                Duration.ofMillis(20));
        assertThrows(GatewayTimeoutException.class, () -> api.getUserByUsername("alice").block());
    }

    @Test
    void rejectsInvalidInputBeforeSendingRequest() {
        var api = new UserApiClient(WebClient.builder().exchangeFunction(request -> {
            fail("Invalid input must not send a request");
            return Mono.empty();
        }).build(), Duration.ofSeconds(1));
        assertThrows(BadRequestException.class, () -> api.getUserByUsername(null).block());
        assertThrows(BadRequestException.class, () -> api.isLogin("alice", "").block());
        assertThrows(BadRequestException.class, () -> api.getUserByUsername(" ").block());
    }

    @Test
    void rejectsEmptyOrInvalidSuccessEnvelope() {
        for (String body : new String[] { "", "{\"success\":false,\"data\":{\"id\":1}}",
                "{\"success\":true,\"data\":null}" }) {
            assertThrows(UpstreamServiceException.class,
                    () -> client(HttpStatus.OK, body).getUserByUsername("alice").block());
        }
    }

    @Test
    void usernameLookupUnwrapsUserAndRemovesPassword() {
        var api = new UserApiClient(WebClient.builder().exchangeFunction(request -> {
            assertEquals("/api/users/username/alice", request.url().getPath());
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                    .body("{\"success\":true,\"data\":{\"username\":\"alice\",\"password\":\"secret\"}}")
                    .build());
        }).build(), Duration.ofSeconds(1));
        var user = api.getUserByUsername("alice").block();
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        assertNull(user.getPassword());
    }

    @Test
    void checksPasswordWithPostAndDecodesFalsePayload() {
        var api = new UserApiClient(WebClient.builder().exchangeFunction(request -> {
            assertEquals(org.springframework.http.HttpMethod.POST, request.method());
            assertEquals("/api/users/isLogin", request.url().getPath());
            assertNull(request.url().getQuery());
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                    .body("{\"success\":true,\"data\":{\"userDto\":null,\"isLogin\":false}}").build());
        }).build(), Duration.ofSeconds(1));
        var result = api.isLogin("alice", "wrong").block();
        assertNotNull(result);
        assertFalse(result.isLogin());
        assertNull(result.userDto());
    }
}
