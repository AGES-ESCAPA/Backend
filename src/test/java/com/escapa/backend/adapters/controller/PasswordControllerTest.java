package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordControllerTest extends PostgresIntegrationTest {

    private static final String URL = "/api/v1/me/password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordHasherPort passwordHasherPort;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(userId, "Senha Test", "senha." + userId + "@test.com",
                passwordHasherPort.hash("senha-atual"), "STUDENT", LocalDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        userJpaRepository.deleteById(userId);
    }

    @Test
    void shouldChangePasswordAndKeepOtherColumns() throws Exception {
        final UserEntity inactive = userJpaRepository.findById(userId).orElseThrow();
        inactive.setStatus(UserStatus.INACTIVE);
        userJpaRepository.save(inactive);

        mockMvc.perform(put(URL)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("senha-atual", "nova-senha-segura")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Password changed successfully")));

        final UserEntity updated = userJpaRepository.findById(userId).orElseThrow();
        assertTrue(passwordHasherPort.matches("nova-senha-segura", updated.getPasswordHash()));
        assertFalse(passwordHasherPort.matches("senha-atual", updated.getPasswordHash()));
        assertEquals(UserStatus.INACTIVE, updated.getStatus());
        assertEquals("Senha Test", updated.getName());
    }

    @Test
    void shouldReturnBadRequestWhenCurrentPasswordIsWrong() throws Exception {
        mockMvc.perform(put(URL)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("senha-errada", "nova-senha-segura")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Current password is incorrect")));

        final String storedHash = userJpaRepository.findById(userId).orElseThrow().getPasswordHash();
        assertTrue(passwordHasherPort.matches("senha-atual", storedHash));
    }

    @Test
    void shouldReturnBadRequestWhenNewPasswordIsTooShort() throws Exception {
        mockMvc.perform(put(URL)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("senha-atual", "curta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Password must be at least 8 characters")));
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingHeader() throws Exception {
        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("senha-atual", "nova-senha-segura")))
                .andExpect(status().isUnauthorized());
    }

    private static String body(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}";
    }
}
