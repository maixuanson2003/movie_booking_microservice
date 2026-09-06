package com.example.user_service.config;

import java.util.Map;
import com.example.user_service.exception.GlobalExceptionHandler;
import com.example.user_service.exception.ResourceNotFoundException;
import com.example.user_service.sharedLogic.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiResponseAdviceTests {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SampleController())
            .setControllerAdvice(new ApiResponseAdvice(), new GlobalExceptionHandler()).build();

    @Test
    void wrapsDataAndPreservesStatus() throws Exception {
        mvc.perform(get("/created")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void wrapsStringsAndNullBodies() throws Exception {
        mvc.perform(get("/text")).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.data").value("hello"))
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(get("/empty")).andExpect(content().json(
                "{\"message\":\"Success\",\"data\":null,\"success\":true}"));
    }

    @Test
    void doesNotWrapExistingResponseOrExceptionTwice() throws Exception {
        mvc.perform(get("/wrapped")).andExpect(jsonPath("$.message").value("Saved"))
                .andExpect(jsonPath("$.data.id").value(1));
        mvc.perform(get("/missing")).andExpect(status().isNotFound())
                .andExpect(content().json(
                        "{\"message\":\"User not found\",\"data\":null,\"success\":false}"));
    }

    @Test
    void explicitErrorUsesSameEnvelopeAndNoContentStaysEmpty() throws Exception {
        mvc.perform(get("/bad")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.reason").value("Invalid input"));
        mvc.perform(get("/no-content")).andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @RestController
    static class SampleController {
        @GetMapping("/created")
        ResponseEntity<Object> created() { return ResponseEntity.status(201).body(Map.of("id", 1)); }
        @GetMapping("/text")
        String text() { return "hello"; }
        @GetMapping("/empty")
        void empty() { }
        @GetMapping("/wrapped")
        ApiResponse wrapped() { return new ApiResponse("Saved", Map.of("id", 1), true); }
        @GetMapping("/missing")
        Object missing() { throw new ResourceNotFoundException("User not found"); }
        @GetMapping("/bad")
        ResponseEntity<Object> bad() { return ResponseEntity.badRequest().body(Map.of("reason", "Invalid input")); }
        @GetMapping("/no-content")
        ResponseEntity<Void> noContent() { return ResponseEntity.noContent().build(); }
    }
}
