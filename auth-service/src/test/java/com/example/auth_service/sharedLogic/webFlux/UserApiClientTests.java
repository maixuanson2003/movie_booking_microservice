package com.example.auth_service.sharedLogic.webFlux;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.auth_service.exception.ExternalServiceException;
import com.example.auth_service.exception.GatewayTimeoutException;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class UserApiClientTests {
    private UserApiClient client(HttpStatus status, String body) {
        return new UserApiClient(WebClient.builder().exchangeFunction(request -> {
            assertEquals("/api/users/1", request.url().getPath());
            return Mono.just(ClientResponse.create(status).header("Content-Type", "application/json")
                    .body(body).build());
        }).build(), Duration.ofSeconds(1));
    }

    @Test
    void decodesUserAndUnwrapsEnvelope() {
        var user = client(HttpStatus.OK,
                "{\"message\":\"OK\",\"success\":true,\"data\":{\"id\":1,\"password\":\"secret\"}}")
                .getUser(1L).block();
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertNull(user.getPassword());
    }

    @Test
    void delegatesRemoteErrorsToBaseWebFluxIncludingEmptyBody() {
        for (String body : new String[] {"", "{\"message\":\"Missing\",\"data\":null,\"success\":false}"}) {
            var error = assertThrows(ExternalServiceException.class,
                    () -> client(HttpStatus.NOT_FOUND, body).getUser(1L).block());
            assertEquals(404, error.getStatus());
        }
    }

    @Test
    void appliesTimeout() {
        var api = new UserApiClient(WebClient.builder().exchangeFunction(request -> Mono.never()).build(),
                Duration.ofMillis(20));
        assertThrows(GatewayTimeoutException.class, () -> api.getUser(1L).block());
    }
}
