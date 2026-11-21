package com.escapa.backend.adapters.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.escapa.backend.adapters.controller.CompanyEmployeeRegistrationIntegrationTest.RecordingEmailSender;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;

/** PUT /api/v1/company/employees/{id} (US-32) de ponta a ponta, com o Postgres real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CompanyEmployeeRegistrationIntegrationTest.EmailTestConfig.class)
class UpdateCompanyEmployeeControllerTest extends PostgresIntegrationTest {

    private static final String EMPLOYEES = "/api/v1/company/employees/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingEmailSender emailSender;

    private final List<UUID> userIds = new ArrayList<>();

    private UUID companyA;
    private UUID companyB;

    @BeforeEach
    void setUp() {
        emailSender.sent.clear();
        emailSender.failNext = false;
        companyA = givenCompany(3);
        companyB = givenCompany(3);
    }

    // O banco é compartilhado entre as classes de teste: cada uma remove o que criou.
    @AfterEach
    void cleanUp() {
        for (final UUID userId : userIds) {
            jdbcTemplate.update("delete from account_invites where user_id = ? or company_id = ?", userId, userId);
            jdbcTemplate.update("delete from users_company where user_id = ? or company_id = ?", userId, userId);
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
    }

    private UUID givenUser(String role, String status) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role, status) values (?, 'Pessoa', ?, 'hash', ?, ?)",
                id, id + "@empresa.com.br", role, status);
        userIds.add(id);
        return id;
    }

    private UUID givenCompany(int seatsTotal) {
        final UUID id = givenUser("COMPANY", "ACTIVE");
        jdbcTemplate.update("insert into company (id, company_name, seats_total) values (?, 'Hotel', ?)", id, seatsTotal);
        return id;
    }

    private UUID givenEmployee(UUID company, String status) {
        final UUID id = givenUser("STUDENT", status);
        jdbcTemplate.update("insert into users_company (user_id, company_id, role) values (?, ?, 'COLABORADOR')", id, company);
        return id;
    }

    private void openInvite(UUID user, UUID company) {
        jdbcTemplate.update(
                "insert into account_invites (id, user_id, company_id, token_hash, expires_at) values (?, ?, ?, ?, ?)",
                UUID.randomUUID(), user, company, UUID.randomUUID().toString(),
                Timestamp.valueOf(LocalDateTime.now().plusDays(3)));
    }

    private String bearer(UUID userId, String role) {
        return "Bearer " + tokenProviderPort.generateToken(userId, userId + "@email.com", role, role);
    }

    private static String body(String name, String email, String jobTitle, String status) {
        return """
                {"name": %s, "email": %s, "jobTitle": %s, "status": %s}
                """.formatted(json(name), json(email), json(jobTitle), json(status));
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private ResultActions updateAs(UUID companyId, String role, UUID employeeId, String body) throws Exception {
        return mockMvc.perform(put(EMPLOYEES + employeeId)
                .header("Authorization", bearer(companyId, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions updateAs(UUID companyId, UUID employeeId, String body) throws Exception {
        return updateAs(companyId, "COMPANY", employeeId, body);
    }

    private String column(String column, UUID userId) {
        return jdbcTemplate.queryForObject("select " + column + " from users where id = ?", String.class, userId);
    }

    private String jobTitleOf(UUID userId) {
        return jdbcTemplate.queryForObject("select role from users_company where user_id = ?", String.class, userId);
    }

    private int openInvites(UUID userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from account_invites where user_id = ? and used_at is null and expires_at > now()",
                Integer.class, userId);
    }

    @Test
    void shouldSaveTheDataAndReturnTheUpdatedEmployee() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");

        updateAs(companyA, employee, body(" Ana Paula Ferreira ", "ANA@empresa.com.br", "Recepção", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(employee.toString())))
                .andExpect(jsonPath("$.data.name", is("Ana Paula Ferreira")))
                .andExpect(jsonPath("$.data.email", is("ana@empresa.com.br")))
                .andExpect(jsonPath("$.data.jobTitle", is("Recepção")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.inviteSent", nullValue()));

        assertEquals("Ana Paula Ferreira", column("name", employee));
        assertEquals("ana@empresa.com.br", column("email", employee));
        assertEquals("Recepção", jobTitleOf(employee));
        assertTrue(emailSender.sent.isEmpty());
    }

    @Test
    void shouldInactivateTheEmployeeKeepTheSeatAndStopTheirToken() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        mockMvc.perform(get("/api/v1/student/enrollments").header("Authorization", bearer(employee, "STUDENT")))
                .andExpect(status().isOk());

        updateAs(companyA, employee, body("Pessoa", employee + "@empresa.com.br", "Recepção", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        assertEquals("INACTIVE", column("status", employee));
        mockMvc.perform(get("/api/v1/student/enrollments").header("Authorization", bearer(employee, "STUDENT")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/company/seats").header("Authorization", bearer(companyA, "COMPANY")))
                .andExpect(jsonPath("$.data.seatsUsed", is(1)))
                .andExpect(jsonPath("$.data.employeesActive", is(0)));
    }

    @Test
    void shouldLetAReactivatedEmployeeUseThePlatformAgain() throws Exception {
        final UUID employee = givenEmployee(companyA, "INACTIVE");
        jdbcTemplate.update("insert into account_invites (id, user_id, company_id, token_hash, expires_at, used_at)"
                        + " values (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), employee, companyA, UUID.randomUUID().toString(),
                Timestamp.valueOf(LocalDateTime.now().minusDays(5)), Timestamp.valueOf(LocalDateTime.now().minusDays(6)));

        updateAs(companyA, employee, body("Pessoa", employee + "@empresa.com.br", "Recepção", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        mockMvc.perform(get("/api/v1/student/enrollments").header("Authorization", bearer(employee, "STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldKeepAPendingEmployeePendingWhenSetActive() throws Exception {
        final UUID employee = givenEmployee(companyA, "PENDING");
        openInvite(employee, companyA);

        updateAs(companyA, employee, body("Pessoa", employee + "@empresa.com.br", "Recepção", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.inviteSent", nullValue()));

        assertEquals("PENDING", column("status", employee));
        assertEquals(1, openInvites(employee));
    }

    @Test
    void shouldSendANewInviteWhenTheEmailOfAPendingEmployeeChanges() throws Exception {
        final UUID employee = givenEmployee(companyA, "PENDING");
        openInvite(employee, companyA);

        updateAs(companyA, employee, body("Pessoa", "novo@empresa.com.br", "Recepção", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("novo@empresa.com.br")))
                .andExpect(jsonPath("$.data.inviteSent", is(true)));

        assertEquals(1, openInvites(employee));
        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from account_invites where user_id = ?", Integer.class, employee));
        assertEquals(1, emailSender.sent.size());
        assertEquals("novo@empresa.com.br", emailSender.sent.get(0).to());
    }

    @Test
    void shouldInvalidateTheInviteWhenAPendingEmployeeIsInactivated() throws Exception {
        final UUID employee = givenEmployee(companyA, "PENDING");
        openInvite(employee, companyA);

        updateAs(companyA, employee, body("Pessoa", employee + "@empresa.com.br", "Recepção", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        assertEquals(0, openInvites(employee));
    }

    @Test
    void shouldReturnConflictForAnEmailInUseByAnotherAccount() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        final UUID other = givenUser("STUDENT", "ACTIVE");

        updateAs(companyA, employee, body("Pessoa", other + "@empresa.com.br", "Recepção", "ACTIVE"))
                .andExpect(status().isConflict());

        assertEquals(employee + "@empresa.com.br", column("email", employee));
    }

    @Test
    void shouldReturnNotFoundForAnEmployeeOfAnotherCompanyAndChangeNothing() throws Exception {
        final UUID outsider = givenEmployee(companyB, "ACTIVE");

        updateAs(companyA, outsider, body("Invasor", "invasor@empresa.com.br", "Recepção", "INACTIVE"))
                .andExpect(status().isNotFound());

        assertEquals("Pessoa", column("name", outsider));
        assertEquals("ACTIVE", column("status", outsider));
    }

    @Test
    void shouldRejectInvalidData() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");

        updateAs(companyA, employee, body("", "ana@empresa.com.br", "Recepção", "ACTIVE")).andExpect(status().isBadRequest());
        updateAs(companyA, employee, body("Ana", "ana", "Recepção", "ACTIVE")).andExpect(status().isBadRequest());
        updateAs(companyA, employee, body("Ana", "ana@empresa.com.br", " ", "ACTIVE")).andExpect(status().isBadRequest());
        updateAs(companyA, employee, body("Ana", "ana@empresa.com.br", "Recepção", null)).andExpect(status().isBadRequest());
        updateAs(companyA, employee, body("Ana", "ana@empresa.com.br", "Recepção", "PENDING")).andExpect(status().isBadRequest());
        updateAs(companyA, employee, body("Ana", "ana@empresa.com.br", "Recepção", "BLOCKED")).andExpect(status().isBadRequest());

        assertEquals("Pessoa", column("name", employee));
    }

    @Test
    void shouldReturnUnauthorizedWithoutAToken() throws Exception {
        mockMvc.perform(put(EMPLOYEES + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ana", "ana@empresa.com.br", "Recepção", "ACTIVE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenForAnyProfileThatIsNotACompany() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        final UUID admin = givenUser("ADMIN", "ACTIVE");
        final String request = body("Ana", "ana@empresa.com.br", "Recepção", "ACTIVE");

        updateAs(employee, "STUDENT", employee, request).andExpect(status().isForbidden());
        updateAs(admin, "ADMIN", employee, request).andExpect(status().isForbidden());
    }
}
