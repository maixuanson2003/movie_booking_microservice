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

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private UserService service;

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
