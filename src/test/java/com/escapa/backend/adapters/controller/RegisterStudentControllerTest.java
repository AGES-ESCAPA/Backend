package com.escapa.backend.adapters.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.escapa.backend.infrastructure.validation.RegistrationContactValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class RegisterStudentControllerTest {

    private static final String REGISTER_URL = "/api/v1/auth/register/student";
    private static ValidatorFactory validatorFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private InMemoryUserRepositoryPort userRepository;
    private InMemoryRegularUserRepositoryPort regularUserRepository;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepositoryPort();
        regularUserRepository = new InMemoryRegularUserRepositoryPort(userRepository);
        final RegisterStudentUseCase registerStudentUseCase = new RegisterStudentUseCase(
                userRepository, regularUserRepository, new FakePasswordHasher(), tokenProvider(),
                new RegistrationContactValidator(validatorFactory.getValidator()));
        final LoginUseCase loginUseCase = new LoginUseCase(userRepository, new FakePasswordHasher(), tokenProvider());
        final AuthController controller = new AuthController(loginUseCase, null, registerStudentUseCase);
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
                .andExpect(jsonPath("$.data.user.profile").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
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
                .andExpect(jsonPath("$.message").value("email already registered"));
    }

    @Test
    void shouldReturn409AndIdentifyDuplicateCpf() throws Exception {
        regularUserRepository.save(
                new User("Existing", "existing@email.com", "hash", "STUDENT"), "52998224725", null);

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("cpf already registered"));
    }

    @Test
    void shouldNormalizeRegistrationAndAllowLogin() throws Exception {
        final String request = validRequest().replace("Maria da Silva", "  Maria da Silva  ")
                .replace("maria@email.com", "  MARIA@EMAIL.COM  ");
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.name").value("Maria da Silva"))
                .andExpect(jsonPath("$.data.user.email").value("maria@email.com"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.profile").value("STUDENT"));
    }

    @Test
    void shouldRejectDuplicateEmailAfterAnActualRegistration() throws Exception {
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isCreated());
        final String duplicate = validRequest().replace("529.982.247-25", "12345678909")
                .replace("maria@email.com", " MARIA@EMAIL.COM ");

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(duplicate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("email already registered"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void shouldRejectInvalidOrMissingFields(String field, String value, String message) throws Exception {
        final Map<String, String> body = new LinkedHashMap<>(Map.of(
                "name", "Maria", "cpf", "52998224725", "email", "maria@email.com", "password", "12345678"));
        if (value == null) {
            body.remove(field);
        } else {
            body.put(field, value);
        }

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(message)));
        assertTrue(userRepository.findAll().isEmpty());
    }

    private static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("name", null, "Name is required"),
                Arguments.of("name", "   ", "Name is required"),
                Arguments.of("cpf", null, "CPF is required"),
                Arguments.of("cpf", " ", "CPF is required"),
                Arguments.of("cpf", "52998224724", "Invalid CPF"),
                Arguments.of("email", null, "Email is required"),
                Arguments.of("email", " ", "Email is required"),
                Arguments.of("email", "a..b@example.com", "Invalid email"),
                Arguments.of("email", "a@-example.com", "Invalid email"),
                Arguments.of("password", null, "Password is required"),
                Arguments.of("password", "        ", "Password is required"),
                Arguments.of("phone", "abc", "Invalid phone"),
                Arguments.of("phone", "+1 123", "Invalid phone"),
                Arguments.of("name", "x".repeat(256), "name must have at most 255 characters"),
                Arguments.of("phone", "1".repeat(256), "phone must have at most 255 characters")
        );
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
