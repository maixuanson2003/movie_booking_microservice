package com.example.cinema_service.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.example.cinema_service.service.jwt.JwtService;
import com.example.cinema_service.sharedLogic.dto.AuthInfo;
import jakarta.servlet.http.Cookie;

@SpringJUnitConfig(SecurityConfigTests.Config.class)
@WebAppConfiguration
class SecurityConfigTests {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, JwtService.class, RequestContext.class, Probe.class})
    static class Config {}

    @RestController
    static class Probe {
        private final RequestContext context;
        Probe(RequestContext context) { this.context = context; }

        @GetMapping("/test/auth")
        String currentUser() {
            return context.getAuthInfo().getUsername() + ":" +
                    SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        }

        @PostMapping("/test/auth")
        String update() { return context.getAuthInfo().getUsername(); }
    }

    @Autowired WebApplicationContext context;
    @Autowired JwtService jwtService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Cookie token() {
        return new Cookie("token", jwtService.createToken(AuthInfo.builder()
                .id(42L).username("alice").role("USER").build()));
    }

    @Test
    void authenticatesAndExposesRequestContextWithoutPersistingSession() throws Exception {
        var result = mvc.perform(get("/test/auth").cookie(token()))
                .andExpect(status().isOk()).andExpect(content().string("alice:ROLE_USER")).andReturn();
        assertNull(result.getRequest().getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        mvc.perform(get("/test/auth")).andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mvc.perform(get("/test/auth").cookie(new Cookie("token", "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preservesCsrfProtectionForCookieAuthenticatedWrites() throws Exception {
        mvc.perform(post("/test/auth").cookie(token())).andExpect(status().isForbidden());
        mvc.perform(post("/test/auth").cookie(token()).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string("alice"));
    }
}
