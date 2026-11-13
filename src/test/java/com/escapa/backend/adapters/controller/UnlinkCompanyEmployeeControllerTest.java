package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.port.CompanyEmployeeUnlinkPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DELETE /api/v1/company/employees/{id} de ponta a ponta, com o SQL real e a transação no Postgres. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UnlinkCompanyEmployeeControllerTest extends PostgresIntegrationTest {

    private static final String EMPLOYEES = "/api/v1/company/employees/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private CompanyEmployeeUnlinkPort unlinkPort;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> courseIds = new ArrayList<>();

    private UUID companyA;
    private UUID companyB;
    private UUID companyCourse;
    private UUID ownCourse;

    // O banco é compartilhado entre as classes de teste: cada uma remove o que criou.
    @BeforeEach
    void setUp() {
        companyA = givenCompany(10);
        companyB = givenCompany(3);
        companyCourse = givenCourse();
        ownCourse = givenCourse();
        ownCourse(companyA, companyCourse);
    }

    @AfterEach
    void cleanUp() {
        for (final UUID userId : userIds) {
            jdbcTemplate.update("delete from orders where buyer_user_id = ?", userId);
            jdbcTemplate.update("delete from account_invites where user_id = ? or company_id = ?", userId, userId);
            jdbcTemplate.update("delete from user_courses where user_id = ?", userId);
            jdbcTemplate.update("delete from company_courses where company_id = ?", userId);
            jdbcTemplate.update("delete from users_company where user_id = ? or company_id = ?", userId, userId);
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
        for (final UUID courseId : courseIds) {
            jdbcTemplate.update("delete from courses where id = ?", courseId);
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
        jdbcTemplate.update("insert into users_company (user_id, company_id, role) values (?, ?, 'Recepção')", id, company);
        return id;
    }

    private UUID givenCourse() {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into courses (id, title, status) values (?, 'Curso', 'PUBLISHED')", id);
        courseIds.add(id);
        return id;
    }

    private void ownCourse(UUID company, UUID course) {
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio) values (?, ?, ?)",
                company, course, java.sql.Date.valueOf(LocalDate.now()));
    }

    private void enroll(UUID user, UUID course, int progress, String certificateCode) {
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, progress, certificate_issued, certificate_code)"
                        + " values (?, ?, ?, ?, ?)",
                user, course, progress, certificateCode != null, certificateCode);
    }

    private void buyOnTheirOwn(UUID user, UUID course) {
        jdbcTemplate.update(
                "insert into orders (id, buyer_user_id, type, item_type, course_id, amount, payment_method, status)"
                        + " values (?, ?, 'INDIVIDUAL', 'COURSE', ?, 100, 'CARD', 'APPROVED')",
                UUID.randomUUID(), user, course);
    }

    private void invite(UUID user, UUID company, boolean used, LocalDateTime expiresAt) {
        jdbcTemplate.update(
                "insert into account_invites (id, user_id, company_id, token_hash, expires_at, used_at)"
                        + " values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), user, company, UUID.randomUUID().toString(), Timestamp.valueOf(expiresAt),
                used ? Timestamp.valueOf(LocalDateTime.now().minusDays(1)) : null);
    }

    private String bearer(UUID userId, String role) {
        return "Bearer " + tokenProviderPort.generateToken(userId, userId + "@email.com", role, role);
    }

    private ResultActions unlinkAs(UUID companyId, UUID employeeId) throws Exception {
        return mockMvc.perform(delete(EMPLOYEES + employeeId).header("Authorization", bearer(companyId, "COMPANY")));
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private String statusOf(UUID userId) {
        return jdbcTemplate.queryForObject("select status from users where id = ?", String.class, userId);
    }

    private Timestamp revokedAt(UUID user, UUID course) {
        return jdbcTemplate.queryForObject(
                "select revoked_at from user_courses where user_id = ? and course_id = ?",
                Timestamp.class, user, course);
    }

    @Test
    void shouldUnlinkTheEmployeeAndReturnTheUpdatedSeats() throws Exception {
        final UUID leaving = givenEmployee(companyA, "ACTIVE");
        givenEmployee(companyA, "ACTIVE");
        givenEmployee(companyA, "PENDING");

        unlinkAs(companyA, leaving)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seatsTotal", is(10)))
                .andExpect(jsonPath("$.data.seatsUsed", is(2)))
                .andExpect(jsonPath("$.data.seatsAvailable", is(8)))
                .andExpect(jsonPath("$.data.employeesTotal", is(2)))
                .andExpect(jsonPath("$.data.employeesActive", is(1)));

        assertEquals(0, count("select count(*) from users_company where user_id = ?", leaving));
        assertEquals("INACTIVE", statusOf(leaving));
    }

    @Test
    void shouldRevokeTheCompanyCoursesButKeepProgressAndCertificates() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        enroll(employee, companyCourse, 100, "CERT-" + UUID.randomUUID());

        unlinkAs(companyA, employee).andExpect(status().isOk());

        assertNotNull(revokedAt(employee, companyCourse));
        assertEquals(1, count("select count(*) from user_courses where user_id = ?", employee));
        assertEquals(100, count("select progress from user_courses where user_id = ?", employee));
        assertEquals(1, count("select count(*) from user_courses where user_id = ? and certificate_issued", employee));
    }

    @Test
    void shouldKeepTheCoursesTheEmployeeBoughtOnTheirOwn() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        enroll(employee, companyCourse, 30, null);
        enroll(employee, ownCourse, 60, null);
        buyOnTheirOwn(employee, ownCourse);

        unlinkAs(companyA, employee).andExpect(status().isOk());

        assertNotNull(revokedAt(employee, companyCourse));
        assertNull(revokedAt(employee, ownCourse));
    }

    @Test
    void shouldKeepAnEnrollmentThatIsAlsoACompanyCourseWhenTheEmployeeBoughtItThemselves() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        ownCourse(companyA, ownCourse);
        enroll(employee, ownCourse, 10, null);
        buyOnTheirOwn(employee, ownCourse);

        unlinkAs(companyA, employee).andExpect(status().isOk());

        assertNull(revokedAt(employee, ownCourse));
    }

    @Test
    void shouldNotRevokeTheEnrollmentsOfOtherEmployees() throws Exception {
        final UUID leaving = givenEmployee(companyA, "ACTIVE");
        final UUID staying = givenEmployee(companyA, "ACTIVE");
        enroll(leaving, companyCourse, 0, null);
        enroll(staying, companyCourse, 0, null);

        unlinkAs(companyA, leaving).andExpect(status().isOk());

        assertNull(revokedAt(staying, companyCourse));
        assertEquals("ACTIVE", statusOf(staying));
        assertEquals(1, count("select count(*) from users_company where user_id = ?", staying));
    }

    @Test
    void shouldInvalidateOnlyThePendingInvitesOfTheEmployee() throws Exception {
        final UUID employee = givenEmployee(companyA, "PENDING");
        invite(employee, companyA, false, LocalDateTime.now().plusDays(3));
        invite(employee, companyA, true, LocalDateTime.now().plusDays(3));

        unlinkAs(companyA, employee).andExpect(status().isOk());

        assertEquals(0, count(
                "select count(*) from account_invites where user_id = ? and used_at is null and expires_at > now()",
                employee));
        // O convite usado e o histórico continuam: é ele que identifica quem já foi colaborador.
        assertEquals(2, count("select count(*) from account_invites where user_id = ?", employee));
        assertEquals("INACTIVE", statusOf(employee));
    }

    @Test
    void shouldUnlinkAnEmployeeWhoIsAlreadyInactive() throws Exception {
        final UUID employee = givenEmployee(companyA, "INACTIVE");

        unlinkAs(companyA, employee)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seatsUsed", is(0)));

        assertEquals("INACTIVE", statusOf(employee));
    }

    @Test
    void shouldStopTheUnlinkedEmployeeFromUsingTheirToken() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        enroll(employee, companyCourse, 0, null);

        mockMvc.perform(get("/api/v1/student/enrollments").header("Authorization", bearer(employee, "STUDENT")))
                .andExpect(status().isOk());

        unlinkAs(companyA, employee).andExpect(status().isOk());

        // A conta inativa é barrada pelo filtro do token, antes de chegar à rota.
        mockMvc.perform(get("/api/v1/student/enrollments").header("Authorization", bearer(employee, "STUDENT")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFreeTheSeatSoAnotherEmployeeCanTakeIt() throws Exception {
        final UUID leaving = givenEmployee(companyB, "ACTIVE");
        givenEmployee(companyB, "ACTIVE");
        givenEmployee(companyB, "ACTIVE");

        unlinkAs(companyB, leaving)
                .andExpect(jsonPath("$.data.seatsUsed", is(2)))
                .andExpect(jsonPath("$.data.seatsAvailable", is(1)));
    }

    @Test
    void shouldReturnNotFoundForAnEmployeeOfAnotherCompanyAndChangeNothing() throws Exception {
        final UUID outsider = givenEmployee(companyB, "ACTIVE");
        enroll(outsider, companyCourse, 0, null);
        invite(outsider, companyB, false, LocalDateTime.now().plusDays(3));

        unlinkAs(companyA, outsider).andExpect(status().isNotFound());

        assertEquals(1, count("select count(*) from users_company where user_id = ?", outsider));
        assertEquals("ACTIVE", statusOf(outsider));
        assertNull(revokedAt(outsider, companyCourse));
        assertEquals(1, count(
                "select count(*) from account_invites where user_id = ? and expires_at > now()", outsider));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownEmployee() throws Exception {
        unlinkAs(companyA, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenUnlinkingTheSameEmployeeTwice() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");

        unlinkAs(companyA, employee).andExpect(status().isOk());
        unlinkAs(companyA, employee).andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnIdThatIsNotAUuid() throws Exception {
        mockMvc.perform(delete(EMPLOYEES + "not-a-uuid").header("Authorization", bearer(companyA, "COMPANY")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUndoTheWholeUnlinkWhenAStepFails() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        enroll(employee, companyCourse, 40, null);
        invite(employee, companyA, false, LocalDateTime.now().plusDays(3));
        doThrow(new IllegalStateException("simulated failure"))
                .when(unlinkPort).invalidatePendingInvites(any(), any(), any());

        unlinkAs(companyA, employee).andExpect(status().isInternalServerError());

        // O vínculo e a revogação já tinham sido feitos quando o último passo falhou: tudo volta.
        assertEquals(1, count("select count(*) from users_company where user_id = ?", employee));
        assertEquals("ACTIVE", statusOf(employee));
        assertNull(revokedAt(employee, companyCourse));
        assertEquals(1, count(
                "select count(*) from account_invites where user_id = ? and used_at is null and expires_at > now()",
                employee));
    }

    @Test
    void shouldReturnUnauthorizedWithoutAToken() throws Exception {
        mockMvc.perform(delete(EMPLOYEES + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenForAnyProfileThatIsNotACompany() throws Exception {
        final UUID employee = givenEmployee(companyA, "ACTIVE");
        final UUID admin = givenUser("ADMIN", "ACTIVE");

        mockMvc.perform(delete(EMPLOYEES + employee).header("Authorization", bearer(employee, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(EMPLOYEES + employee).header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isForbidden());

        assertEquals(1, count("select count(*) from users_company where user_id = ?", employee));
        assertEquals("ACTIVE", statusOf(employee));
    }
}
