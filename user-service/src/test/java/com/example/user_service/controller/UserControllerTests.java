package com.example.user_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.example.user_service.config.SecurityConfig;
import com.example.user_service.exception.ResourceNotFoundException;
import com.example.user_service.service.UserService;
import com.example.user_service.sharedLogic.dto.CheckPasswordRequest;
import com.example.user_service.sharedLogic.dto.UserDTO;
import com.example.user_service.sharedLogic.dto.LoginResult;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, com.example.user_service.service.jwt.JwtService.class})
class UserControllerTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private UserService service;

    @Test
    void middlewareMakesAuthInfoAvailableToServiceAndDoesNotLeakToNextRequest() throws Exception {
        var jwtService = new com.example.user_service.service.jwt.JwtService();
        String token = jwtService.createToken(com.example.user_service.sharedLogic.dto.AuthInfo.builder()
                .id(42L).username("alice").role("USER").build());
        when(service.getUserByUsername("alice")).thenAnswer(invocation -> {
            var info = new com.example.user_service.config.RequestContext().getAuthInfo();
            org.junit.jupiter.api.Assertions.assertEquals(42L, info.getId());
            return new UserDTO();
        });
        mvc.perform(get("/api/users/username/alice").cookie(new jakarta.servlet.http.Cookie("token", token)))
                .andExpect(status().isOk());
        when(service.getUserByUsername("bob")).thenAnswer(invocation -> {
            org.junit.jupiter.api.Assertions.assertNull(new com.example.user_service.config.RequestContext().getAuthInfo());
            org.junit.jupiter.api.Assertions.assertEquals("anonymousUser",
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            return new UserDTO();
        });
        mvc.perform(get("/api/users/username/bob")).andExpect(status().isOk());
    }

    @Test
    void registrationIsAccessibleAndReturnsCreatedUser() throws Exception {
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        when(service.registerUser(any())).thenReturn(new LoginResult(dto, true));
        mvc.perform(post("/api/users/register").contentType("application/json")
                .content("{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"correct\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLogin").value(true))
                .andExpect(jsonPath("$.data.userDto.username").value("alice"));
        verify(service).registerUser(argThat(request -> "alice@example.com".equals(request.getEmail())
                && "correct".equals(request.getPassword())));
    }

    @Test
    void loginReturnsUserAndExactIsLoginField() throws Exception {
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        when(service.isLogin("alice", "correct")).thenReturn(new LoginResult(dto, true));
        mvc.perform(post("/api/users/isLogin").contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"correct\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLogin").value(true))
                .andExpect(jsonPath("$.data.userDto.username").value("alice"))
                .andExpect(jsonPath("$.data.userDto.password").doesNotExist());
        when(service.isLogin("alice", "wrong")).thenReturn(new LoginResult(null, false));
        mvc.perform(post("/api/users/isLogin").contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLogin").value(false))
                .andExpect(jsonPath("$.data.userDto").doesNotExist());
    }

    @Test
    void anonymousLookupReturnsEnvelope() throws Exception {
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        when(service.getUserByUsername("alice")).thenReturn(dto);
        mvc.perform(get("/api/users/username/alice"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void anonymousPostChecksBodyWithoutCsrfTokenAndPreservesFalse() throws Exception {
        var request = new CheckPasswordRequest("alice", "wrong");
        when(service.checkPassword(request)).thenReturn(false);
        mvc.perform(post("/api/users/check-password").contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
        verify(service).checkPassword(request);
    }

    @Test
    void missingUserAndMalformedJsonUseErrorEnvelope() throws Exception {
        when(service.getUserByUsername("missing")).thenThrow(new ResourceNotFoundException("User not found"));
        mvc.perform(get("/api/users/username/missing"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/api/users/check-password").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unrelatedEndpointIsNotOpened() throws Exception {
        mvc.perform(get("/api/users/private")).andExpect(status().isForbidden());
    }
}
