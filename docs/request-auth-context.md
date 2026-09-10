# Authentication context

`booking-service`, `cinema-service`, `email-service`, `movie-service`, and
`payment-service` use the same cookie JWT middleware as `user-service`.
JWT middleware is configured in the backend services only. `api-gateway` and
`auth-service` are outside the scope of this configuration.

The middleware reads the `token` cookie, validates the signed JWT, converts its
`auth` claim to `AuthInfo`, and derives Spring Security authorities from
`authInfo.role`. Invalid tokens return HTTP 401. JWT format and signing key match
the existing `user-service` implementation.

## MVC services

Inject the service's `RequestContext` into a controller or service:

```java
private final RequestContext requestContext;

public MyService(RequestContext requestContext) {
    this.requestContext = requestContext;
}

public void handleRequest() {
    AuthInfo authInfo = requestContext.getAuthInfo();
    Long userId = authInfo.getId();
}
```

The value is stored as a servlet request attribute named `authInfo`. It is
available during the MVC request, not in unrelated background jobs or Kafka
consumers. Public requests without a token have no `AuthInfo`. Security contexts
are stateless and are not reused for subsequent requests.

The five added backend security chains require authentication for all endpoints.
CSRF protection remains enabled for writes authenticated by cookies, consistent
with `user-service` outside its existing public login and registration endpoints.

## Verification

Each added MVC service has middleware tests and Spring Security/MockMvc tests
covering identity access, request isolation, invalid tokens, and CSRF protection.
These tests run without a database, Kafka broker, or Eureka server:

```shell
mvn test -Dtest=AuthMiddlewareTests,SecurityConfigTests
```
