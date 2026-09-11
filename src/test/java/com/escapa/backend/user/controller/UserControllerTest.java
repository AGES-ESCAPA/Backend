package com.escapa.backend.user.controller;

import com.escapa.backend.common.WebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de /api/v1/users: rotas, status, envelope e nomes dos campos.
 * Emails únicos por teste para não depender de limpeza entre eles.
 */
class UserControllerTest extends WebIntegrationTest {

    @Test
    void shouldCreateUserAndAnswer201WithEnvelope() throws Exception {
        final String email = uniqueEmail("nova");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Nova Usuaria", email.toUpperCase(), "password123", "student")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("Nova Usuaria"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.userType").value("STUDENT"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldAnswer400WithValidationCodeWhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "nao-e-email", "curta", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/v1/users"));
    }

    @Test
    void shouldAnswer409WithCodeWhenEmailAlreadyUsed() throws Exception {
        final String email = uniqueEmail("dup");
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Primeira", email, "password123", "student")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Segunda", email, "password123", "teacher")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    void shouldAnswer404WithCodeForUnknownUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void shouldAnswer400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/users/nao-e-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldListUsersInsideEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(isA(java.util.List.class)));
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@email.com";
    }

    private static String body(String name, String email, String password, String userType) {
        return """
                {"name":"%s","email":"%s","password":"%s","userType":"%s"}
                """.formatted(name, email, password, userType);
    }
}
