package com.escapa.backend.adapters.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.escapa.backend.application.usecase.GetProfileUseCase;
import com.escapa.backend.application.usecase.InMemoryCompanyRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUsersCompanyRepositoryPort;
import com.escapa.backend.application.usecase.UpdateProfileUseCase;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;

class ProfileControllerTest {

    private static final String PROFILE_URL = "/api/v1/me/profile";

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companyRepository = new InMemoryCompanyRepositoryPort(userRepository);
    private final InMemoryUsersCompanyRepositoryPort usersCompanyRepository = new InMemoryUsersCompanyRepositoryPort();

    private MockMvc mockMvc;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = userRepository.save(new User("Mariana Costa", "mariana@email.com", "hash", "STUDENT")).getId();
        userRepository.saveStudentContact(studentId, "39011122233", "51991203344");

        final ProfileController controller = new ProfileController(
                new GetProfileUseCase(userRepository, companyRepository, usersCompanyRepository),
                new UpdateProfileUseCase(userRepository, companyRepository, usersCompanyRepository));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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

    private UUID saveEmployee() {
        final UUID companyId = UUID.randomUUID();
        companyRepository.save(
                new User(companyId, "Ana Responsavel", "contato@vistamar.com.br", "hash", "COMPANY", null, null),
                new Company(companyId, "Pousada Vista Mar LTDA", "12345678000191", "rh@vistamar.com.br", null));
        final UUID employeeId = userRepository.save(
                new User("Luciana Prado", "luciana@vistamar.com.br", "hash", "STUDENT")).getId();
        userRepository.saveStudentContact(employeeId, "39077788899", "54999998888");
        usersCompanyRepository.linkToCompany(employeeId, companyId);
        return employeeId;
    }

    @Test
    void shouldRejectRequestsWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get(PROFILE_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid access token"));
    }

    @Test
    void shouldReturnStudentProfileInEnvelopeWithoutCompanyFields() throws Exception {
        mockMvc.perform(get(PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile").value("STUDENT"))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andExpect(jsonPath("$.data.name").value("Mariana Costa"))
                .andExpect(jsonPath("$.data.email").value("mariana@email.com"))
                .andExpect(jsonPath("$.data.cpf").value("39011122233"))
                .andExpect(jsonPath("$.data.phone").value("51991203344"))
                .andExpect(jsonPath("$.data.companyName").doesNotExist())
                .andExpect(jsonPath("$.data.cnpj").doesNotExist())
                .andExpect(jsonPath("$.data.avatarUrl").doesNotExist());
    }

    @Test
    void shouldReturnTheSavedAvatarUrl() throws Exception {
        userRepository.findById(studentId).orElseThrow().setAvatarUrl("http://minio/avatars/mariana.png");

        mockMvc.perform(get(PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value("http://minio/avatars/mariana.png"));
    }

    @Test
    void shouldReturnEmployeeProfileInTheIssueFormat() throws Exception {
        authenticate(saveEmployee(), "STUDENT");

        mockMvc.perform(get(PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.editable").value(false))
                .andExpect(jsonPath("$.data.cpf").value("39077788899"))
                .andExpect(jsonPath("$.data.companyName").value("Pousada Vista Mar LTDA"));
    }

    @Test
    void shouldUpdateAllowedFieldsAndIgnoreNonEditableOnes() throws Exception {
        mockMvc.perform(put(PROFILE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mariana Souza", "phone": "(54) 99999-8888",
                                 "email": "outro@email.com", "cpf": "00000000000", "profile": "ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Mariana Souza"))
                .andExpect(jsonPath("$.data.phone").value("54999998888"))
                .andExpect(jsonPath("$.data.email").value("mariana@email.com"))
                .andExpect(jsonPath("$.data.cpf").value("39011122233"))
                .andExpect(jsonPath("$.data.profile").value("STUDENT"));

        assertEquals("mariana@email.com", userRepository.findById(studentId).orElseThrow().getEmail());
    }

    @Test
    void shouldForbidEmployeeFromEditing() throws Exception {
        authenticate(saveEmployee(), "STUDENT");

        mockMvc.perform(put(PROFILE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Outro Nome\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnFieldMessageWhenNameIsBlank() throws Exception {
        mockMvc.perform(put(PROFILE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \", \"phone\": \"54999998888\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name is required"));
    }

    @Test
    void shouldReturnFieldMessageWhenNameIsTooLong() throws Exception {
        mockMvc.perform(put(PROFILE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name must have at most 255 characters"));
    }

    @Test
    void shouldReturnBadRequestWhenPhoneIsInvalid() throws Exception {
        mockMvc.perform(put(PROFILE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Mariana Costa\", \"phone\": \"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("phone is invalid"));
    }
}
