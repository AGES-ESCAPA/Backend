package com.escapa.backend.adapters.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.usecase.DeactivateAccountUseCase;
import com.escapa.backend.application.usecase.FakePasswordHasher;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUsersCompanyRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;

class MeControllerTest {

    private static final String DEACTIVATE_URL = "/api/v1/me/deactivate";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryUsersCompanyRepositoryPort usersCompany = new InMemoryUsersCompanyRepositoryPort();
    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();

    private MockMvc mockMvc;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = users.save(new User(
                "Mariana Costa",
                "mariana@email.com",
                passwordHasher.hash("SenhaAtual1!"),
                "STUDENT")).getId();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MeController(new DeactivateAccountUseCase(users, usersCompany, passwordHasher)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate(studentId, "STUDENT");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(UUID userId, String role) {
        final var user = new AuthenticatedUser(userId, "test@escapa.com", role, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Test
    void shouldDeactivateAuthenticatedAccount() throws Exception {
        mockMvc.perform(post(DEACTIVATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"SenhaAtual1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deactivated successfully"));

        assertEquals(UserStatus.INACTIVE, users.findById(studentId).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post(DEACTIVATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"errada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void shouldRejectBlankPassword() throws Exception {
        mockMvc.perform(post(DEACTIVATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post(DEACTIVATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"SenhaAtual1!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidCompanyEmployeeSelfDeactivation() throws Exception {
        usersCompany.linkToCompany(studentId, UUID.randomUUID());

        mockMvc.perform(post(DEACTIVATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"SenhaAtual1!\"}"))
                .andExpect(status().isForbidden());
    }
}
