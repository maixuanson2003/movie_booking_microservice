package com.example.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Wiring smoke test without external PostgreSQL, Eureka or Kafka connections.
@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"eureka.client.enabled=false",
		"spring.kafka.listener.auto-startup=false",
		"auth.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
		"APP_DB_PASSWORD=test-only"
})
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
