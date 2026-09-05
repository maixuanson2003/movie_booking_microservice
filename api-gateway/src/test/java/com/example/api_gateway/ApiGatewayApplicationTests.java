package com.example.api_gateway;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTests {
    @Autowired
    RouteLocator locator;

    @Test
    void apiPathsMatchExpectedEurekaServices() {
        assertRoute("/api/auth/login", "auth-service");
        assertRoute("/auth/login", "auth-service");
        assertRoute("/movies/42", "movie-service");
        assertRoute("/bookings/1", "booking-service");
        assertRoute("/api/users", "user-service");
        assertRoute("/api/movies/42?language=vi", "movie-service");
        assertRoute("/api/seats/12", "cinema-service");
        assertRoute("/api/showtimes", "cinema-service");
        assertRoute("/api/bookings", "booking-service");
        assertRoute("/api/combos/1", "booking-service");
        assertRoute("/api/invoices/1", "payment-service");
        assertRoute("/api/payment-methods", "payment-service");
    }

    private void assertRoute(String path, String service) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        var routes = locator.getRoutes()
            .filterWhen(route -> route.getPredicate().apply(exchange))
            .collectList().block(Duration.ofSeconds(5));
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getUri().toString()).isEqualTo("lb://" + service);
        assertThat(exchange.getRequest().getURI().toString()).endsWith(path);
    }

    @Test
    void doesNotExposeAutomaticServiceNameRoutes() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/movie-service/api/movies"));
        var routes = locator.getRoutes()
            .filterWhen(route -> route.getPredicate().apply(exchange))
            .collectList().block(Duration.ofSeconds(5));
        assertThat(routes).isEmpty();
    }
}
