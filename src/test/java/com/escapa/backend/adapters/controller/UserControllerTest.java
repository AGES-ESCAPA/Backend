package com.escapa.backend.adapters.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.usecase.AdminDeactivateUserUseCase;
import com.escapa.backend.application.usecase.AdminUpdateUserUseCase;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.EmailChangeMailer;
import com.escapa.backend.application.usecase.FakeContactValidator;
import com.escapa.backend.application.usecase.FakePasswordHasher;
import com.escapa.backend.application.usecase.GetProfileUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.InMemoryAccountInviteRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryCompanyRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryEmailChangeTokenRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserProfileChangeLogRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUsersCompanyRepositoryPort;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.RequestAdminEmailChangeUseCase;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;

/** Mapeamento HTTP do cadastro e da consulta de usuários. */
class UserControllerTest {

    private static final String USERS_URL = "/api/v1/users";
    private static final String VALID_BODY = """
            {"name": "Maria Silva", "email": "Maria@Email.com", "password": "12345678", "userType": "student"}
            """;

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companyRepository = new InMemoryCompanyRepositoryPort(userRepository);
    private final InMemoryUsersCompanyRepositoryPort usersCompanyRepository = new InMemoryUsersCompanyRepositoryPort();
    private final InMemoryAccountInviteRepositoryPort invites = new InMemoryAccountInviteRepositoryPort();
    private final InMemoryEmailChangeTokenRepositoryPort emailChangeTokens =
            new InMemoryEmailChangeTokenRepositoryPort();
    private final InMemoryUserProfileChangeLogRepositoryPort changeLog =
            new InMemoryUserProfileChangeLogRepositoryPort();

    private MockMvc mockMvc;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        final Clock clock = Clock.systemUTC();
        final UserController controller = new UserController(
                new CreateUserUseCase(userRepository, new FakePasswordHasher()),
                new ListUsersUseCase(userRepository),
                new GetUserByIdUseCase(userRepository),
                new GetProfileUseCase(userRepository, companyRepository, usersCompanyRepository),
                new AdminUpdateUserUseCase(
                        userRepository, companyRepository, usersCompanyRepository, changeLog),
                new AdminDeactivateUserUseCase(userRepository, invites, clock),
                new RequestAdminEmailChangeUseCase(
                        userRepository, emailChangeTokens, new FakeContactValidator(), clock, 72),
                new EmailChangeMailer((to, subject, html, text) -> { }, (template, vars) -> "", "http://localhost:3000"),
                emailChangeTokens,
                clock
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        final User admin = userRepository.save(new User("Admin Escapa", "admin@escapa.com", "hash", "ADMIN"));
        adminId = admin.getId();
        authenticate(adminId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateAUserWithNormalizedDataAndHideThePassword() throws Exception {
        mockMvc.perform(post(USERS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.email").value("maria@email.com"))
                .andExpect(jsonPath("$.data.userType").value("STUDENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectInvalidBodyWith400() throws Exception {
        mockMvc.perform(post(USERS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"x\", \"password\": \"1\", \"userType\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectADuplicatedEmail() throws Exception {
        mockMvc.perform(post(USERS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post(USERS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    void shouldListAndGetUsers() throws Exception {
        final User saved = userRepository.save(new User("Maria Silva", "maria@email.com", "hash", "STUDENT"));
        userRepository.saveStudentContact(saved.getId(), "39011122233", "51991203344");

        mockMvc.perform(get(USERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.email=='maria@email.com')].status", hasItem("ACTIVE")))
                .andExpect(jsonPath("$.data[?(@.email=='maria@email.com')].userType", hasItem("STUDENT")))
                .andExpect(jsonPath("$.data[?(@.email=='maria@email.com')].cpf").doesNotExist());
        mockMvc.perform(get(USERS_URL + "/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Maria Silva"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.cpf").value("39011122233"))
                .andExpect(jsonPath("$.data.phone").value("51991203344"));
    }

    @Test
    void shouldListACompanyLinkedStudentAsEmployee() throws Exception {
        final User companyUser = userRepository.save(
                new User("RH Vista Mar", "rh@vistamar.com.br", "hash", "COMPANY"));
        companyRepository.save(
                companyUser,
                new Company(companyUser.getId(), "Pousada Vista Mar", "12345678000191", "rh@vistamar.com.br", null));

        final User linked = userRepository.save(
                new User("Luciana Prado", "luciana@vistamar.com.br", "hash", "STUDENT"));
        userRepository.linkUserToCompany(linked.getId());
        usersCompanyRepository.linkToCompany(linked.getId(), companyUser.getId());

        mockMvc.perform(get(USERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.email=='luciana@vistamar.com.br')].userType", hasItem("EMPLOYEE")));
        mockMvc.perform(get(USERS_URL + "/{id}", linked.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userType").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.companyName").value("Pousada Vista Mar"));
    }

    @Test
    void shouldReturnCompanyRegistrationFieldsOnDetail() throws Exception {
        final User companyUser = userRepository.save(
                new User("João Responsável", "rh@vistamar.com.br", "hash", "COMPANY"));
        companyRepository.save(
                companyUser,
                new Company(
                        companyUser.getId(),
                        "Pousada Vista Mar LTDA",
                        "12345678000191",
                        "rh@vistamar.com.br",
                        "5133334444"));

        mockMvc.perform(get(USERS_URL + "/{id}", companyUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userType").value("COMPANY"))
                .andExpect(jsonPath("$.data.name").value("João Responsável"))
                .andExpect(jsonPath("$.data.companyName").value("Pousada Vista Mar LTDA"))
                .andExpect(jsonPath("$.data.cnpj").value("12345678000191"))
                .andExpect(jsonPath("$.data.phone").value("5133334444"));
    }

    @Test
    void shouldUpdateStudentRegistrationFields() throws Exception {
        final User saved = userRepository.save(new User("Maria Silva", "maria@email.com", "hash", "STUDENT"));
        userRepository.saveStudentContact(saved.getId(), "39011122233", "51991203344");

        mockMvc.perform(put(USERS_URL + "/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria Atualizada","phone":"51988887777","companyName":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Maria Atualizada"))
                .andExpect(jsonPath("$.data.phone").value("51988887777"))
                .andExpect(jsonPath("$.data.cpf").value("39011122233"));
    }

    @Test
    void shouldRequestEmailChangeAndExposePendingEmailOnDetail() throws Exception {
        final User saved = userRepository.save(new User("Maria Silva", "maria@email.com", "hash", "STUDENT"));

        mockMvc.perform(post(USERS_URL + "/{id}/email-change", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria.nova@email.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("maria@email.com"))
                .andExpect(jsonPath("$.data.pendingEmail").value("maria.nova@email.com"))
                .andExpect(jsonPath("$.message").value(
                        "Confirmation e-mail sent. The address will change after confirmation."));
    }

    @Test
    void shouldDeactivateAUser() throws Exception {
        final User saved = userRepository.save(new User("Maria Silva", "maria@email.com", "hash", "STUDENT"));

        mockMvc.perform(post(USERS_URL + "/{id}/deactivate", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.message").value("User deactivated successfully"));
    }

    @Test
    void shouldRejectDeactivatingOwnAccount() throws Exception {
        mockMvc.perform(post(USERS_URL + "/{id}/deactivate", adminId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Administrators cannot deactivate their own account"));
    }

    @Test
    void shouldReturn404ForAnUnknownUser() throws Exception {
        mockMvc.perform(get(USERS_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private static void authenticate(UUID userId) {
        final var user = new AuthenticatedUser(userId, "admin@escapa.com", "ADMIN", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of()));
    }
}
