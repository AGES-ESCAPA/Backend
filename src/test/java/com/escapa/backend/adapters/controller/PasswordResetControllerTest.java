package com.escapa.backend.adapters.controller;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.port.EmailTemplatePort;
import com.escapa.backend.application.usecase.FakeEmailSender;
import com.escapa.backend.application.usecase.FakePasswordHasher;
import com.escapa.backend.application.usecase.GetPasswordResetTokenUseCase;
import com.escapa.backend.application.usecase.InMemoryPasswordResetTokenRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.PasswordResetMailer;
import com.escapa.backend.application.usecase.RequestPasswordResetUseCase;
import com.escapa.backend.application.usecase.ResetPasswordUseCase;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

/** Rotas públicas de recuperação de senha, com os casos de uso reais sobre fakes em memória. */
class PasswordResetControllerTest {

    private static final String BASE = "/api/v1/auth/password-reset";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK =
            Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final String REQUEST_MESSAGE =
            "Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha.";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryPasswordResetTokenRepositoryPort tokens = new InMemoryPasswordResetTokenRepositoryPort();
    private final FakeEmailSender emailSender = new FakeEmailSender();

    private MockMvc mockMvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = users.save(new User(
                null, "Ana", "ana@escapa.com", "old-hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, NOW.minusDays(1))).getId();
        givenToken("token-valido", NOW.plusHours(2), null);
        givenToken("token-usado", NOW.plusHours(2), NOW.minusMinutes(5));
        givenToken("token-expirado", NOW.minusMinutes(1), null);

        final EmailTemplatePort templates = (name, variables) -> name + "|" + variables.get("link");
        final PasswordResetController controller = new PasswordResetController(
                new RequestPasswordResetUseCase(users, tokens, CLOCK, 2),
                new GetPasswordResetTokenUseCase(tokens, users, CLOCK),
                new ResetPasswordUseCase(tokens, users, new FakePasswordHasher(), CLOCK),
                new PasswordResetMailer(emailSender, templates, "https://app.escapa.com.br"));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldAcceptAResetRequestForAKnownEmail() throws Exception {
        mockMvc.perform(post(BASE + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"ana@escapa.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(REQUEST_MESSAGE));
    }

    @Test
    void shouldReturnTheSameMessageForAnUnknownEmail() throws Exception {
        mockMvc.perform(post(BASE + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nobody@escapa.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(REQUEST_MESSAGE));
    }

    @Test
    void shouldReturnTheEmailForAValidToken() throws Exception {
        mockMvc.perform(get(BASE + "/token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ana@escapa.com"))
                .andExpect(jsonPath("$.message").value("Operation completed successfully"));
    }

    @Test
    void shouldReturn404ForAnUnknownOrUsedToken() throws Exception {
        mockMvc.perform(get(BASE + "/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Password reset token not found"));
        mockMvc.perform(get(BASE + "/token-usado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Password reset token not found"));
    }

    @Test
    void shouldReturn410ForAnExpiredToken() throws Exception {
        mockMvc.perform(get(BASE + "/token-expirado"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Password reset token has expired"));
    }

    @Test
    void shouldResetThePasswordWithAValidToken() throws Exception {
        resetPassword("token-valido", "senha-segura")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    void shouldReturn400ForAShortPassword() throws Exception {
        resetPassword("token-valido", "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters"));
    }

    @Test
    void shouldReturn410WhenResettingWithAnExpiredToken() throws Exception {
        resetPassword("token-expirado", "senha-segura")
                .andExpect(status().isGone());
    }

    @Test
    void shouldReturn404WhenTheSameTokenIsUsedTwice() throws Exception {
        resetPassword("token-valido", "senha-segura").andExpect(status().isOk());

        resetPassword("token-valido", "outra-senha-segura")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Password reset token not found"));
    }

    private ResultActions resetPassword(String token, String password) throws Exception {
        return mockMvc.perform(post(BASE + "/" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"" + password + "\"}"));
    }

    private void givenToken(String rawToken, LocalDateTime expiresAt, LocalDateTime usedAt) {
        tokens.save(new PasswordResetToken(
                null,
                userId,
                PasswordResetToken.hashToken(rawToken),
                expiresAt,
                usedAt,
                NOW.minusHours(1)));
    }
}
