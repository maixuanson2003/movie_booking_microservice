package com.example.user_service.middleware;

import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.user_service.config.RequestContext;
import com.example.user_service.service.jwt.JwtService;
import com.example.user_service.sharedLogic.dto.AuthInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

public class AuthMiddleware extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public AuthMiddleware(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token != null && !token.isBlank()) {
            try {
                Boolean isValid = jwtService.isTokenValid(token);
                if (!isValid) {
                    throw new IllegalArgumentException("Token is expired or invalid");
                }
                Claims claims = jwtService.getAllClaim(token);
                AuthInfo authInfo = jsonMapper.convertValue(claims.get("auth"), AuthInfo.class);
                if (claims.getExpiration() == null || authInfo == null
                        || authInfo.getUsername() == null || authInfo.getUsername().isBlank()
                        || !authInfo.getUsername().equals(claims.getSubject())) {
                    throw new IllegalArgumentException("Invalid auth claim");
                }
                String role = authInfo.getRole();
                List<GrantedAuthority> authorities = role == null || role.isBlank()
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(authInfo.getUsername(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Filters run before DispatcherServlet binds RequestContextHolder.
                request.setAttribute(RequestContext.AUTH_INFO, authInfo);
            } catch (JwtException | IllegalArgumentException | tools.jackson.core.JacksonException ex) {
                SecurityContextHolder.clearContext();
                request.removeAttribute(RequestContext.AUTH_INFO);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired authentication token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
