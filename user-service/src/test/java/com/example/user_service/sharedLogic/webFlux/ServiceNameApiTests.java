// package com.example.user_service.sharedLogic.webFlux;

// import java.net.ConnectException;
// import java.net.URI;
// import java.time.Duration;
// import org.junit.jupiter.api.Test;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpMethod;
// import org.springframework.http.HttpStatus;
// import org.springframework.web.reactive.function.client.ClientResponse;
// import org.springframework.web.reactive.function.client.WebClient;
// import
// org.springframework.web.reactive.function.client.WebClientRequestException;
// import com.example.user_service.exception.*;
// import reactor.core.publisher.Mono;
// import static org.junit.jupiter.api.Assertions.*;

// class ServiceNameApiTests {
// private serviceNameApi client(int status, String body) {
// WebClient webClient = WebClient.builder().exchangeFunction(request -> {
// assertEquals("/users/1", request.url().getPath());
// return Mono.just(ClientResponse.create(HttpStatus.valueOf(status))
// .header("Content-Type", "application/json").body(body).build());
// }).build();
// return new serviceNameApi(webClient, Duration.ofSeconds(2));
// }

// @Test
// void unwrapsTypedUserAndDoesNotForwardPassword() {
// var user = client(200,
// "{\"message\":\"OK\",\"success\":true,\"data\":{\"id\":1,\"username\":\"demo\",\"password\":\"secret\"}}")
// .getUser(1L).block();
// assertNotNull(user);
// assertEquals(1L, user.getId());
// assertEquals("demo", user.getUsername());
// assertNull(user.getPassword());
// }

// @Test
// void mapsHttpErrorsWithoutExposingRemoteBody() {
// int[] statuses = {404, 409, 401, 403, 500, 503, 504};
// int[] expected = {404, 409, 502, 502, 502, 503, 504};
// for (int i = 0; i < statuses.length; i++) {
// serviceNameApi api = client(statuses[i], "remote-secret");
// BusinessException error = assertThrows(BusinessException.class, () ->
// api.getUser(1L).block());
// assertEquals(expected[i], error.getStatus().value());
// assertFalse(error.getMessage().contains("remote-secret"));
// }
// }

// @Test
// void mapsConnectionFailureAndTimeout() {
// WebClient failed = WebClient.builder().exchangeFunction(request ->
// Mono.error(
// new WebClientRequestException(new ConnectException(), HttpMethod.GET,
// URI.create("http://service/users/1"), HttpHeaders.EMPTY))).build();
// assertThrows(ServiceUnavailableException.class,
// () -> new serviceNameApi(failed, Duration.ofSeconds(1)).getUser(1L).block());
// WebClient hanging = WebClient.builder().exchangeFunction(request ->
// Mono.never()).build();
// assertThrows(GatewayTimeoutException.class,
// () -> new serviceNameApi(hanging,
// Duration.ofMillis(20)).getUser(1L).block());
// }

// @Test
// void rejectsMalformedEmptyAndUnsuccessfulResponses() {
// for (String body : new String[] {"", "{", "{}",
// "{\"success\":false,\"data\":null}",
// "{\"success\":true,\"data\":null}"}) {
// assertThrows(UpstreamServiceException.class, () -> client(200,
// body).getUser(1L).block());
// }
// }

// @Test
// void rejectsInvalidIdBeforeSendingRequest() {
// WebClient unused = WebClient.builder().exchangeFunction(request -> {
// fail("Must not call service for invalid id");
// return Mono.empty();
// }).build();
// serviceNameApi api = new serviceNameApi(unused, Duration.ofSeconds(1));
// assertThrows(BadRequestException.class, () -> api.getUser(null).block());
// assertThrows(BadRequestException.class, () -> api.getUser(0L).block());
// }
// }
