package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.FakePasswordHasher;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP do cadastro e da consulta de usuários. */
class UserControllerTest {

    private static final String USERS_URL = "/api/v1/users";
    private static final String VALID_BODY = """
            {"name": "Maria Silva", "email": "Maria@Email.com", "password": "12345678", "userType": "student"}
            """;

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final UserController controller = new UserController(
                new CreateUserUseCase(userRepository, new FakePasswordHasher()),
                new ListUsersUseCase(userRepository),
                new GetUserByIdUseCase(userRepository)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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

        mockMvc.perform(get(USERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get(USERS_URL + "/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Maria Silva"));
    }

    @Test
    void shouldReturn404ForAnUnknownUser() throws Exception {
        mockMvc.perform(get(USERS_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
