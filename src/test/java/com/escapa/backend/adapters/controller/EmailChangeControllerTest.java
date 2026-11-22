package com.escapa.backend.adapters.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.usecase.ConfirmEmailChangeUseCase;
import com.escapa.backend.application.usecase.GetEmailChangeTokenUseCase;
import com.escapa.backend.application.usecase.InMemoryEmailChangeTokenRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserProfileChangeLogRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class EmailChangeControllerTest {

    private static final String BASE = "/api/v1/auth/email-change";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK =
            Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryEmailChangeTokenRepositoryPort tokens = new InMemoryEmailChangeTokenRepositoryPort();
    private final InMemoryUserProfileChangeLogRepositoryPort changeLog =
            new InMemoryUserProfileChangeLogRepositoryPort();

    private MockMvc mockMvc;
    private UUID userId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        actorId = users.save(new User(
                null, "Admin", "admin@escapa.com", "hash", UserTypes.ADMIN, null,
                UserStatus.ACTIVE, NOW.minusDays(1))).getId();
        userId = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, NOW.minusDays(1))).getId();
        givenToken("token-valido", "ana.nova@escapa.com", NOW.plusHours(2), null);
        givenToken("token-usado", "usado@escapa.com", NOW.plusHours(2), NOW.minusMinutes(5));
        givenToken("token-expirado", "expirado@escapa.com", NOW.minusMinutes(1), null);

        mockMvc = MockMvcBuilders.standaloneSetup(new EmailChangeController(
                        new GetEmailChangeTokenUseCase(tokens, users, CLOCK),
                        new ConfirmEmailChangeUseCase(tokens, users, changeLog, CLOCK)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnTokenDetails() throws Exception {
        mockMvc.perform(get(BASE + "/token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Ana"))
                .andExpect(jsonPath("$.data.currentEmail").value("ana@escapa.com"))
                .andExpect(jsonPath("$.data.newEmail").value("ana.nova@escapa.com"));
    }

    @Test
    void shouldReturnNotFoundForUnknownOrUsedToken() throws Exception {
        mockMvc.perform(get(BASE + "/inexistente")).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/token-usado")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnGoneForExpiredToken() throws Exception {
        mockMvc.perform(get(BASE + "/token-expirado")).andExpect(status().isGone());
    }

    @Test
    void shouldConfirmAndPersistTheNewEmail() throws Exception {
        mockMvc.perform(post(BASE + "/token-valido/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email updated successfully"));

        assertEquals("ana.nova@escapa.com", users.findById(userId).orElseThrow().getEmail());
        assertEquals(1, changeLog.entries().size());
    }

    private void givenToken(String raw, String newEmail, LocalDateTime expiresAt, LocalDateTime usedAt) {
        tokens.save(new EmailChangeToken(
                null,
                userId,
                newEmail,
                EmailChangeToken.hashToken(raw),
                actorId,
                expiresAt,
                usedAt,
                NOW));
    }
}
