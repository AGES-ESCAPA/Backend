package com.escapa.backend.infrastructure.security;

import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminMetricsSecurityIntegrationTest extends PostgresIntegrationTest {
    private static final String URL = "/api/v1/admin/metrics/overview";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TokenProviderPort tokens;

    private UUID adminId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        adminId = user("ADMIN");
        studentId = user("STUDENT");
        user("COMPANY");
        final UUID courseId = UUID.randomUUID();
        final LocalDate today = LocalDate.now();
        jdbc.update("insert into courses (id, title, status) values (?, 'Metrics', 'PUBLISHED')", courseId);
        jdbc.update("insert into user_courses (user_id, course_id, dt_inicio, dt_expiracao, progress) "
                + "values (?, ?, ?, ?, 100)", studentId, courseId, today, today);
    }

    @Test
    void shouldRequireValidBearerToken() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectStudentToken() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(studentId, "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnMetricsForAdminToken() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeStudents").value(1))
                .andExpect(jsonPath("$.data.partnerCompanies").value(1))
                .andExpect(jsonPath("$.data.averageCompletionRate").value(100.00))
                .andExpect(jsonPath("$.data.activeStudentsByMonth.length()").value(6))
                .andExpect(jsonPath("$.data.activeStudentsByMonth[5].activeStudents").value(1));
    }

    private UUID user(String role) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into users (id, name, email, password_hash, role) "
                + "values (?, 'Metrics', ?, 'hash', ?)", id, id + "@example.com", role);
        return id;
    }

    private String bearer(UUID id, String role) {
        return "Bearer " + tokens.generateToken(id, id + "@example.com", role, role);
    }
}
