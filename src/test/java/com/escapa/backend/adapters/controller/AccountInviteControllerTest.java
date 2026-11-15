package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.model.TokenClaims;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.usecase.CreateInvitePasswordUseCase;
import com.escapa.backend.application.usecase.FakePasswordHasher;
import com.escapa.backend.application.usecase.GetAccountInviteUseCase;
import com.escapa.backend.application.usecase.InMemoryAccountInviteRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryCompanyRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rotas públicas do convite (US-31), com os casos de uso reais sobre os fakes em memória. */
class AccountInviteControllerTest {

    private static final String INVITES = "/api/v1/auth/invites/";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companies = new InMemoryCompanyRepositoryPort(users);
    private final InMemoryAccountInviteRepositoryPort invites = new InMemoryAccountInviteRepositoryPort();

    private MockMvc mockMvc;
    private UUID companyId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        companies.save(
                new User(companyId, "Marta Lima", "contato@vistamar.com.br", "hash", "COMPANY", null,
                        UserStatus.ACTIVE, NOW.minusDays(30)),
                new Company(companyId, "Pousada Vista Mar LTDA", "12345678000199", "contato@vistamar.com.br", null));
        employeeId = users.save(new User(null, "João da Silva", "joao.silva@empresa.com.br",
                "PENDING-NO-PASSWORD-SET", "STUDENT", null, UserStatus.PENDING, NOW.minusDays(1))).getId();
        givenInvite("convite-valido", NOW.plusHours(72), null);
        givenInvite("convite-usado", NOW.plusHours(72), NOW.minusHours(1));
        givenInvite("convite-expirado", NOW.minusHours(1), null);

        final AccountInviteController controller = new AccountInviteController(
                new GetAccountInviteUseCase(invites, users, companies, CLOCK),
                new CreateInvitePasswordUseCase(invites, users, new FakePasswordHasher(), new StubTokenProvider(), CLOCK));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnTheInviteDetails() throws Exception {
        mockMvc.perform(get(INVITES + "convite-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("João da Silva"))
                .andExpect(jsonPath("$.data.email").value("joao.silva@empresa.com.br"))
                .andExpect(jsonPath("$.data.companyName").value("Pousada Vista Mar LTDA"))
                .andExpect(jsonPath("$.message").value("Operation completed successfully"));
    }

    @Test
    void shouldReturn404ForAnUnknownOrUsedInvite() throws Exception {
        mockMvc.perform(get(INVITES + "token-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invite not found"));
        mockMvc.perform(get(INVITES + "convite-usado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invite not found"));
    }

    @Test
    void shouldReturn410ForAnExpiredInvite() throws Exception {
        mockMvc.perform(get(INVITES + "convite-expirado"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Invite has expired"));
    }

    @Test
    void shouldCreateThePasswordAndReturnTheLoginPayload() throws Exception {
        createPassword("convite-valido", "senha-segura")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("fake-jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.id").value(employeeId.toString()))
                .andExpect(jsonPath("$.data.user.email").value("joao.silva@empresa.com.br"))
                .andExpect(jsonPath("$.data.user.profile").value("EMPLOYEE"));
    }

    @Test
    void shouldReturn400ForAShortPassword() throws Exception {
        createPassword("convite-valido", "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters"));
    }

    @Test
    void shouldReturn410WhenCreatingThePasswordWithAnExpiredInvite() throws Exception {
        createPassword("convite-expirado", "senha-segura")
                .andExpect(status().isGone());
    }

    @Test
    void shouldReturn404WhenTheSameInviteIsUsedTwice() throws Exception {
        createPassword("convite-valido", "senha-segura").andExpect(status().isOk());

        createPassword("convite-valido", "outra-senha-segura")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invite not found"));
    }

    private ResultActions createPassword(String token, String password) throws Exception {
        return mockMvc.perform(post(INVITES + token + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"" + password + "\"}"));
    }

    private void givenInvite(String token, LocalDateTime expiresAt, LocalDateTime usedAt) {
        invites.save(new AccountInvite(null, employeeId, companyId, AccountInvite.hashToken(token),
                expiresAt, usedAt, NOW.minusDays(1)));
    }

    /** Token fixo: o JWT de verdade é coberto pelos testes do JwtTokenProviderAdapter. */
    private static final class StubTokenProvider implements TokenProviderPort {

        @Override
        public String generateToken(UUID userId, String email, String role, String profile) {
            return "fake-jwt-token";
        }

        @Override
        public TokenClaims validateToken(String token) {
            throw new UnsupportedOperationException("Not used by the invite routes");
        }

        @Override
        public long getExpirationSeconds() {
            return 3600L;
        }
    }
}
