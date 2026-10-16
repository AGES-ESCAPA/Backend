package com.escapa.backend.adapters.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.usecase.FakePasswordHasher;
import com.escapa.backend.application.usecase.InMemoryRegularUserRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.LoginUseCase;
import com.escapa.backend.application.usecase.RegisterStudentUseCase;
import com.escapa.backend.domain.entity.User;

class RegisterStudentControllerTest {

    private static final String REGISTER_URL = "/api/v1/auth/register/student";

    private MockMvc mockMvc;
    private InMemoryUserRepositoryPort userRepository;
    private InMemoryRegularUserRepositoryPort regularUserRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepositoryPort();
        regularUserRepository = new InMemoryRegularUserRepositoryPort();
        final RegisterStudentUseCase registerStudentUseCase = new RegisterStudentUseCase(
                userRepository, regularUserRepository, new FakePasswordHasher(), tokenProvider());
        final LoginUseCase loginUseCase = new LoginUseCase(null, new FakePasswordHasher(), null);
        final AuthController controller = new AuthController(loginUseCase, registerStudentUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn201WithTokenAndStudentData() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("student-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.id").isNotEmpty())
                .andExpect(jsonPath("$.data.user.name").value("Maria da Silva"))
                .andExpect(jsonPath("$.data.user.email").value("maria@email.com"))
                .andExpect(jsonPath("$.data.user.profile").value("STUDENT"));
    }

    @Test
    void shouldReturn400ForInvalidCpf() throws Exception {
        final String request = validRequest().replace("529.982.247-25", "111.111.111-11");

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid CPF"));
    }

    @Test
    void shouldReturn400ForInvalidEmail() throws Exception {
        final String request = validRequest().replace("maria@email.com", "email-invalido");

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email"));
    }

    @Test
    void shouldReturn400ForShortPassword() throws Exception {
        final String request = validRequest().replace("12345678", "1234567");

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters"));
    }

    @Test
    void shouldReturn409AndIdentifyDuplicateEmail() throws Exception {
        userRepository.save(new User("Existing", "maria@email.com", "hash", "STUDENT"));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void shouldReturn409AndIdentifyDuplicateCpf() throws Exception {
        regularUserRepository.save(
                new User("Existing", "existing@email.com", "hash", "STUDENT"), "52998224725", null);

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CPF already registered"));
    }

    private static TokenProviderPort tokenProvider() {
        return new TokenProviderPort() {
            @Override
            public String generateToken(UUID userId, String email, String role, String profile) {
                return "student-token";
            }

            @Override
            public long getExpirationSeconds() {
                return 3600L;
            }
        };
    }

    private static String validRequest() {
        return """
                {
                    "name": "Maria da Silva",
                    "cpf": "529.982.247-25",
                    "email": "maria@email.com",
                    "phone": "(51) 99999-0000",
                    "password": "12345678"
                }
                """;
    }
}
