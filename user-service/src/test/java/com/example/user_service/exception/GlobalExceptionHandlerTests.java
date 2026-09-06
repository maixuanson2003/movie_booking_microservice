package com.example.user_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTests {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void businessExceptionsUseTheirHttpStatusAndCurrentResponseShape() throws Exception {
        String[] kinds = {"bad", "missing", "conflict", "unauthorized", "forbidden", "unavailable"};
        int[] statuses = {400, 404, 409, 401, 403, 503};
        for (int i = 0; i < kinds.length; i++) {
            mvc.perform(get("/errors/" + kinds[i]))
                    .andExpect(status().is(statuses[i]))
                    .andExpect(jsonPath("$.message").value("Public message"))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Test
    void unexpectedErrorsDoNotExposeInternalDetails() throws Exception {
        mvc.perform(get("/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"message\":\"Internal server error\",\"data\":null,\"success\":false}"));
    }

    @Test
    void databaseErrorsReturnConflictWithoutSqlDetails() throws Exception {
        mvc.perform(get("/errors/database"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Data conflicts with existing records or database constraints"));
    }

    @Test
    void frameworkErrorsKeepTheirStatusAndUseApiResponse() throws Exception {
        mvc.perform(post("/errors/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(put("/errors/body"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/errors/status"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }

    @RestController
    static class ErrorController {
        @GetMapping("/errors/{kind}")
        void fail(@PathVariable String kind) {
            throw switch (kind) {
                case "bad" -> new BadRequestException("Public message");
                case "missing" -> new ResourceNotFoundException("Public message");
                case "conflict" -> new ConflictException("Public message");
                case "unauthorized" -> new UnauthorizedException("Public message");
                case "forbidden" -> new ForbiddenException("Public message");
                case "unavailable" -> new ServiceUnavailableException("Public message");
                case "database" -> new DataIntegrityViolationException("SQL secret");
                case "status" -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
                default -> new IllegalStateException("Internal secret");
            };
        }

        @PostMapping("/errors/body")
        void body(@RequestBody java.util.Map<String, String> body) {
        }
    }
}
