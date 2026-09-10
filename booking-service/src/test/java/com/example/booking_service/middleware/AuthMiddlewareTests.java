package com.example.booking_service.middleware;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.booking_service.config.RequestContext;
import com.example.booking_service.service.jwt.JwtService;
import com.example.booking_service.sharedLogic.dto.AuthInfo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;

class AuthMiddlewareTests {
    private final JwtService jwtService = new JwtService();
    private final AuthMiddleware middleware = new AuthMiddleware(jwtService);

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private String validToken() {
        return jwtService.createToken(AuthInfo.builder().id(42L).username("alice")
                .email("alice@example.com").fullName("Alice").role("USER").build());
    }

    private MockHttpServletRequest request(String token) {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", token));
        return request;
    }

    @Test
    void exposesTypedAuthInfoThroughoutRequestWithoutPreviouslyBoundRequestContext() throws Exception {
        var request = request(validToken());
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();
        middleware.doFilter(request, response, (req, res) -> {
            called.set(true);
            // Simulate DispatcherServlet exposing this request to controllers/services.
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            AuthInfo info = new RequestContext().getAuthInfo();
            assertEquals(42L, info.getId());
            assertEquals("alice", info.getUsername());
            assertEquals("alice@example.com", info.getEmail());
            assertSame(info, new RequestContext().getContextValue("authInfo"));
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("alice", authentication.getName());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        });
        assertTrue(called.get());
        assertEquals(200, response.getStatus());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        assertNull(new RequestContext().getAuthInfo());
    }

    @Test
    void missingCookieContinuesWithoutAuthentication() throws Exception {
        var called = new AtomicBoolean();
        middleware.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (req, res) -> called.set(true));
        assertTrue(called.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsMalformedExpiredAndMissingAuthTokens() throws Exception {
        String expired = Jwts.builder().setSubject("alice")
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtService.SECRET))).compact();
        String missingAuth = Jwts.builder().setSubject("alice")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtService.SECRET))).compact();
        String mismatchedAuth = Jwts.builder().setSubject("bob")
                .claim("auth", Map.of("username", "alice"))
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtService.SECRET))).compact();
        for (String token : new String[] {"bad-token", expired, missingAuth, mismatchedAuth}) {
            var request = request(token);
            var response = new MockHttpServletResponse();
            middleware.doFilter(request, response, (req, res) -> fail("Invalid token reached controller"));
            assertEquals(401, response.getStatus());
            assertNull(request.getAttribute(RequestContext.AUTH_INFO));
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Test
    void downstreamErrorsAreNotConvertedToAuthenticationErrors() {
        var response = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> middleware.doFilter(request(validToken()), response,
                (req, res) -> { throw new ServletException("Controller failed"); }));
        assertEquals(200, response.getStatus());
    }
}

