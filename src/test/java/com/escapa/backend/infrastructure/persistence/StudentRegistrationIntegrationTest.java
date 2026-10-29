package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.RegisterStudentCommand;
import com.escapa.backend.application.usecase.UploadAvatarUseCase;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.CpfValidator;
import com.escapa.backend.infrastructure.persistence.StudentRegistrationTestConfig.RegistrationRepositoryControl;
import com.escapa.backend.infrastructure.persistence.StudentRegistrationTestConfig.RegistrationTokenControl;
import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;
import com.escapa.backend.infrastructure.security.JwtTokenProviderAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(StudentRegistrationTestConfig.class)
class StudentRegistrationIntegrationTest extends PostgresIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register/student";
    private static final String PASSWORD = "  senha-segura  ";
    private static final int CPF_BODY_LENGTH = 9;
    private static final int CPF_FIRST_CHECK_INDEX = 9;
    private static final int CPF_SECOND_CHECK_INDEX = 10;
    private static final int CPF_CHECK_BASE = 11;
    private static final long CPF_BODY_RANGE = 1_000_000_000L;

    @Autowired private MockMvc mockMvc;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired @Qualifier("userRepositoryPort") private UserRepositoryPort users;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private RegularUserJpaRepository regularUsers;
    @Autowired private PasswordHasherPort passwords;
    @Autowired private JwtTokenProviderAdapter jwt;
    @Autowired private UploadAvatarUseCase uploadAvatar;
    @Autowired private RegistrationRepositoryControl repositoryControl;
    @Autowired private RegistrationTokenControl tokenControl;
    @Autowired private JdbcTemplate jdbc;

    private String suffix;
    private String cpf;
    private String otherCpf;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString();
        cpf = uniqueCpf();
        otherCpf = uniqueCpf();
        while (otherCpf.equals(cpf)) {
            otherCpf = uniqueCpf();
        }
        repositoryControl.reset();
        tokenControl.reset();
    }

    @AfterEach
    void cleanUp() {
        repositoryControl.reset();
        tokenControl.reset();
        jdbc.update("DELETE FROM users WHERE id IN (SELECT user_id FROM regular_users WHERE cpf IN (?, ?))",
                cpf, otherCpf);
        jdbc.update("DELETE FROM users WHERE email LIKE ?", "%" + suffix + "@example.com");
    }

    @Test
    void shouldRegisterPersistBothTablesAndAuthenticateWithTheSamePassword() throws Exception {
        final String email = "student-" + suffix + "@example.com";
        final RegisterStudentCommand command = command("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", cpf);
        final MvcResult result = register(command);
        assertEquals(201, result.getResponse().getStatus());
        final JsonNode data = response(result).path("data");
        final User saved = users.findByEmail(email).orElseThrow();
        final RegularUserEntity profile = regularUsers.findById(saved.getId()).orElseThrow();

        assertEquals("Student Test", saved.getName());
        assertEquals("STUDENT", saved.getUserType());
        assertEquals(cpf, profile.getCpf());
        assertEquals("51999990000", profile.getPhone());
        assertTrue(passwords.matches(PASSWORD, saved.getPasswordHash()));
        assertEquals(saved.getId().toString(), data.path("user").path("id").asText());
        assertEquals("STUDENT", data.path("user").path("profile").asText());
        assertFalse(data.path("user").has("passwordHash"));
        assertStudentToken(data.path("accessToken").asText(), saved);

        final MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        assertEquals(saved.getId().toString(), response(login).path("data").path("user").path("id").asText());
        assertStudentToken(response(login).path("data").path("accessToken").asText(), saved);
    }

    @Test
    void shouldRollbackBothTablesWhenTokenGenerationFails() throws Exception {
        final String email = "rollback-" + suffix + "@example.com";
        tokenControl.failGeneration();

        final MvcResult result = register(command(email, cpf));

        assertEquals(500, result.getResponse().getStatus());
        assertNotNull(tokenControl.attemptedUserId());
        assertFalse(userJpaRepository.existsById(tokenControl.attemptedUserId()));
        assertFalse(regularUsers.existsById(tokenControl.attemptedUserId()));
        assertFalse(users.existsByEmail(email));
        assertEquals(0, countUsers());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldResolveConcurrentRegistrationsWithoutOrphanUsers(boolean sameEmail) throws Exception {
        final String email = "cpf-" + suffix + "@example.com";
        final RegisterStudentCommand first = command(email, cpf);
        final RegisterStudentCommand second = command(
                sameEmail ? email : "other-" + suffix + "@example.com", sameEmail ? otherCpf : cpf);
        repositoryControl.synchronizeSaves();

        final List<ResponseEntity<String>> results;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            final Future<ResponseEntity<String>> firstRequest = executor.submit(() -> registerHttp(first));
            final Future<ResponseEntity<String>> secondRequest = executor.submit(() -> registerHttp(second));
            results = List.of(firstRequest.get(30, TimeUnit.SECONDS), secondRequest.get(30, TimeUnit.SECONDS));
        }

        assertEquals(List.of(201, 409), httpStatuses(results), () -> httpBodies(results));
        final ResponseEntity<String> conflict = results.stream()
                .filter(result -> result.getStatusCode().value() == 409).findFirst().orElseThrow();
        assertEquals((sameEmail ? "email" : "cpf") + " already registered",
                objectMapper.readTree(conflict.getBody()).path("message").asText());
        assertEquals(1, countUsers());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM regular_users r JOIN users u ON u.id = r.user_id "
                + "WHERE u.email LIKE ?", Integer.class, "%" + suffix + "@example.com"));
    }

    @Test
    void shouldPreserveRegisteredCpfAndPhoneWhenUpdatingAvatar() throws Exception {
        final String email = "avatar-" + suffix + "@example.com";
        assertEquals(201, register(command(email, cpf)).getResponse().getStatus());
        final UUID id = users.findByEmail(email).orElseThrow().getId();

        final User updated = uploadAvatar.execute(id, new ByteArrayInputStream(new byte[] {1, 2, 3}), 3L, "image/png");

        final RegularUserEntity profile = regularUsers.findById(id).orElseThrow();
        assertEquals(cpf, profile.getCpf());
        assertEquals("51999990000", profile.getPhone());
        assertEquals(updated.getAvatarUrl(), profile.getProfileAvatarUrl());
        assertTrue(updated.getAvatarUrl().startsWith("https://storage.test/avatars/"));
    }

    @Test
    void shouldDocumentCreatedAndConflictResponses() throws Exception {
        final String responses = "$.paths['/api/v1/auth/register/student'].post.responses";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(responses + "['201']").exists())
                .andExpect(jsonPath(responses + "['409'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ApiError"));
    }

    private MvcResult register(RegisterStudentCommand command) throws Exception {
        return mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command))).andReturn();
    }

    private ResponseEntity<String> registerHttp(RegisterStudentCommand command) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(REGISTER_URL, new HttpEntity<>(command, headers), String.class);
    }

    private static List<Integer> httpStatuses(List<ResponseEntity<String>> results) {
        return results.stream().map(result -> result.getStatusCode().value()).sorted().toList();
    }

    private static String httpBodies(List<ResponseEntity<String>> results) {
        return results.stream()
                .map(result -> result.getStatusCode().value() + " " + result.getBody())
                .toList()
                .toString();
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static RegisterStudentCommand command(String email, String cpf) {
        return new RegisterStudentCommand("  Student Test  ", cpf, email, "(51) 99999-0000", PASSWORD);
    }

    private static String uniqueCpf() {
        final String body = String.format("%0" + CPF_BODY_LENGTH + "d",
                Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), CPF_BODY_RANGE));
        final int firstCheck = checkDigit(body, CPF_FIRST_CHECK_INDEX);
        final String generated = body + firstCheck + checkDigit(body + firstCheck, CPF_SECOND_CHECK_INDEX);
        return CpfValidator.isValid(generated) ? generated : uniqueCpf();
    }

    private static int checkDigit(String digits, int digitIndex) {
        int sum = 0;
        for (int index = 0; index < digitIndex; index++) {
            sum += Character.digit(digits.charAt(index), 10) * (digitIndex + 1 - index);
        }
        final int result = CPF_CHECK_BASE - sum % CPF_CHECK_BASE;
        return result >= 10 ? 0 : result;
    }

    private int countUsers() {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE ?", Integer.class,
                "%" + suffix + "@example.com");
    }

    private void assertStudentToken(String token, User user) {
        final Claims claims = Jwts.parser().verifyWith(jwt.getKey()).build().parseSignedClaims(token).getPayload();
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals(user.getEmail(), claims.get("email", String.class));
        assertEquals("STUDENT", claims.get("role", String.class));
        assertEquals("STUDENT", claims.get("profile", String.class));
        assertEquals(jwt.getExpirationSeconds() * 1000,
                claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
    }
}
