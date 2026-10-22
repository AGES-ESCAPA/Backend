package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.usecase.FakePasswordHasher;
import com.escapa.backend.application.usecase.LoginOutput;
import com.escapa.backend.application.usecase.LoginUseCase;
import com.escapa.backend.application.usecase.RegisterStudentUseCase;
import com.escapa.backend.application.usecase.RegisterCompanyUseCase;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.InvalidCredentialsException;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";

    private MockMvc mockMvc;
    private LoginUseCase loginUseCase;
    private RegisterCompanyUseCase registerCompanyUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(null, new FakePasswordHasher(), null) {
            @Override
            public LoginOutput execute(String email, String password) {
                if (email == null || email.isBlank() || password == null || password.isBlank()) {
                    throw new InvalidCredentialsException();
                }
                if ("inexistente@email.com".equalsIgnoreCase(email) || "errada".equals(password)) {
                    throw new InvalidCredentialsException("Invalid email or password");
                }
                if ("inativo@email.com".equalsIgnoreCase(email)) {
                    throw new InactiveUserException("User account is inactive");
                }
                final String profile = "funcionario@empresa.com".equalsIgnoreCase(email) ? "EMPLOYEE" : "STUDENT";
                final User user = new User(
                        UUID.fromString("3f1c2b9e-0000-4000-a000-000000000000"),
                        "Nome do Usuario",
                        email,
                        "hash",
                        "STUDENT",
                        null,
                        UserStatus.ACTIVE,
                        LocalDateTime.now()
                );
                return new LoginOutput("fake-jwt-token", "Bearer", 3600L, user, profile);
            }
        };

        registerCompanyUseCase = new RegisterCompanyUseCase(null, null, null, null, null) {
            @Override
            public LoginOutput execute(
                    String responsibleName, String cnpj, String companyName,
                    String email, String phone, String password) {
                if ("11111111111111".equals(cnpj)) {
                    throw new IllegalArgumentException("cnpj is invalid");
                }
                if ("duplicado@empresa.com".equalsIgnoreCase(email)) {
                    throw new DuplicateRegistrationException("email");
                }
                final User user = new User(
                        UUID.fromString("3f1c2b9e-0000-4000-a000-000000000001"),
                        responsibleName,
                        email,
                        "hash",
                        "COMPANY",
                        null,
                        UserStatus.ACTIVE,
                        LocalDateTime.now()
                );
                return new LoginOutput("fake-company-jwt", "Bearer", 3600L, user, "COMPANY");
            }
        };

        final RegisterStudentUseCase registerStudentUseCase = new RegisterStudentUseCase(null, null, null, null, null);
        final AuthController controller = new AuthController(
                loginUseCase, registerCompanyUseCase, registerStudentUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200WithAccessTokenAndUserOnValidCredentials() throws Exception {
        final String requestBody = """
                {
                    "email": "mariana@email.com",
                    "password": "senha-correta"
                }
                """;

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Operation completed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("fake-jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.id").value("3f1c2b9e-0000-4000-a000-000000000000"))
                .andExpect(jsonPath("$.data.user.email").value("mariana@email.com"))
                .andExpect(jsonPath("$.data.user.profile").value("STUDENT"));
    }

    @Test
    void shouldReturnEmployeeProfileWhenUserIsCompanyLinked() throws Exception {
        final String requestBody = """
                {
                    "email": "funcionario@empresa.com",
                    "password": "senha-correta"
                }
                """;

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.profile").value("EMPLOYEE"));
    }

    @Test
    void shouldReturn400WhenPayloadHasInvalidFormat() throws Exception {
        final String invalidBody = """
                {
                    "email": "invalid-email-format",
                    "password": ""
                }
                """;

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        final String requestBody = """
                {
                    "email": "inexistente@email.com",
                    "password": "senha"
                }
                """;

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldReturn403WhenUserIsInactive() throws Exception {
        final String requestBody = """
                {
                    "email": "inativo@email.com",
                    "password": "senha"
                }
                """;

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User account is inactive"));
    }

        private static final String REGISTER_COMPANY_URL = "/api/v1/auth/register/company";

    @Test
    void shouldReturn201WithTokenAndCompanyProfileOnCompanyRegistration() throws Exception {
        final String requestBody = """
                {
                    "responsibleName": "Maria Souza",
                    "cnpj": "11222333000181",
                    "companyName": "Hotel Vista Mar LTDA",
                    "email": "rh@vistamar.com.br",
                    "phone": "51999990001",
                    "password": "senha1234"
                }
                """;

        mockMvc.perform(post(REGISTER_COMPANY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("fake-company-jwt"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.name").value("Maria Souza"))
                .andExpect(jsonPath("$.data.user.email").value("rh@vistamar.com.br"))
                .andExpect(jsonPath("$.data.user.profile").value("COMPANY"));
    }

    @Test
    void shouldReturn400WhenCompanyRegistrationHasMissingFields() throws Exception {
        final String requestBody = """
                {
                    "cnpj": "11222333000181",
                    "companyName": "Hotel Vista Mar LTDA",
                    "email": "rh@vistamar.com.br",
                    "password": "senha1234"
                }
                """;

        mockMvc.perform(post(REGISTER_COMPANY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCompanyRegistrationHasShortPassword() throws Exception {
        final String requestBody = """
                {
                    "responsibleName": "Maria Souza",
                    "cnpj": "11222333000181",
                    "companyName": "Hotel Vista Mar LTDA",
                    "email": "rh@vistamar.com.br",
                    "password": "1234567"
                }
                """;

        mockMvc.perform(post(REGISTER_COMPANY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUseCaseRejectsCnpj() throws Exception {
        final String requestBody = """
                {
                    "responsibleName": "Maria Souza",
                    "cnpj": "11111111111111",
                    "companyName": "Hotel Vista Mar LTDA",
                    "email": "rh@vistamar.com.br",
                    "password": "senha1234"
                }
                """;

        mockMvc.perform(post(REGISTER_COMPANY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenEmailIsAlreadyRegistered() throws Exception {
        final String requestBody = """
                {
                    "responsibleName": "Maria Souza",
                    "cnpj": "11222333000181",
                    "companyName": "Hotel Vista Mar LTDA",
                    "email": "duplicado@empresa.com",
                    "password": "senha1234"
                }
                """;

        mockMvc.perform(post(REGISTER_COMPANY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }
}

