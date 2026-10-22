package com.escapa.backend.infrastructure.security;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenSecurityIntegrationTest extends PostgresIntegrationTest {

    private static final String STUDENT_EMAIL = "student.security@escapa.com";
    private static final String ADMIN_EMAIL = "admin.security@escapa.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UUID studentId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
                studentId, "Student Security", STUDENT_EMAIL, "hash", "STUDENT", LocalDateTime.now()));
        userJpaRepository.save(new UserEntity(
                adminId, "Admin Security", ADMIN_EMAIL, "hash", "ADMIN", LocalDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        userJpaRepository.deleteById(studentId);
        userJpaRepository.deleteById(adminId);
    }

    @Test
    void shouldReturnApiErrorWhenProtectedRouteHasNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/v1/student/enrollments")));
    }

    @Test
    void shouldRequireTokenForAuthenticatedUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/v1/auth/me")));
    }

    @Test
    void shouldReturnApiErrorWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void shouldBlockStudentFromAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void shouldAuthorizeAdminAndUseBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnLoggedInUserFromToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(studentId.toString())))
                .andExpect(jsonPath("$.data.name", is("Student Security")))
                .andExpect(jsonPath("$.data.email", is(STUDENT_EMAIL)))
                .andExpect(jsonPath("$.data.profile", is("STUDENT")));
    }

    @Test
    void shouldProtectUserManagementWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAuthorizationHeaderFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("Authorization")));
    }

    private String studentToken() {
        return tokenProviderPort.generateToken(studentId, STUDENT_EMAIL, "STUDENT", "STUDENT");
    }

    private String adminToken() {
        return tokenProviderPort.generateToken(adminId, ADMIN_EMAIL, "ADMIN", "ADMIN");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
