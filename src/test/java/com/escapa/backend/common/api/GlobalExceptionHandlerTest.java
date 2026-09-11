package com.escapa.backend.common.api;

import com.escapa.backend.common.exception.BusinessRuleException;
import com.escapa.backend.common.exception.ConflictException;
import com.escapa.backend.common.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre cada família de erro do handler sem subir o contexto Spring:
 * um controller de mentira lança a exceção e o teste confere status, {@code code}
 * e se detalhe interno vazou ou não.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- 1. validação de entrada ---------------------------------------

    @Test
    void validationFailureReturns400WithFieldMessages() throws Exception {
        mockMvc.perform(post("/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Name is required"));
    }

    @Test
    void typeMismatchReturns400NamingTheParameter() throws Exception {
        mockMvc.perform(get("/errors/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void missingRequiredParameterReturns400() throws Exception {
        mockMvc.perform(get("/errors/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Missing required parameter 'q'"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    // --- 2. regra de negócio --------------------------------------------

    @Test
    void notFoundReturns404WithFeatureCode() throws Exception {
        mockMvc.perform(get("/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("THING_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Thing 1 not found"))
                .andExpect(jsonPath("$.path").value("/errors/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void conflictReturns409WithFeatureCode() throws Exception {
        mockMvc.perform(get("/errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("THING_ALREADY_EXISTS"));
    }

    @Test
    void businessRuleReturns422WithFeatureCode() throws Exception {
        mockMvc.perform(get("/errors/rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("THING_RULE_BROKEN"));
    }

    // --- 3. protocolo HTTP ----------------------------------------------

    @Test
    void unknownResourceReturns404() throws Exception {
        mockMvc.perform(get("/errors/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found: /errors/no-resource"));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(post("/errors/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/errors/validate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    // --- 4. infraestrutura ----------------------------------------------

    @Test
    void dataIntegrityViolationReturns409WithoutLeakingConstraintName() throws Exception {
        mockMvc.perform(get("/errors/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Request conflicts with existing data"))
                .andExpect(jsonPath("$.message").value(not(containsString("uk_secret"))));
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingDetail() throws Exception {
        mockMvc.perform(get("/errors/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"))
                .andExpect(jsonPath("$.message").value(not(containsString("secret"))));
    }

    // --- controller de apoio --------------------------------------------

    @RestController
    @RequestMapping("/errors")
    static class ThrowingController {

        record Payload(@NotBlank(message = "Name is required") String name) {
        }

        @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validate(@Valid @RequestBody Payload payload) {
        }

        @GetMapping("/typed/{id}")
        void typed(@PathVariable UUID id) {
        }

        @GetMapping("/required")
        void required(@RequestParam String q) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("THING_NOT_FOUND", "Thing 1 not found");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("THING_ALREADY_EXISTS", "Thing already exists");
        }

        @GetMapping("/rule")
        void rule() {
            throw new BusinessRuleException("THING_RULE_BROKEN", "Thing cannot do that");
        }

        @GetMapping("/no-resource")
        void noResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "/errors/no-resource");
        }

        @GetMapping("/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_secret\"");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
