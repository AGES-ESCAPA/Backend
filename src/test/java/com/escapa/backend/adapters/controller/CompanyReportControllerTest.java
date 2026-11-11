package com.escapa.backend.adapters.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/v1/company/reports/overview e /employees de ponta a ponta, com a consulta SQL real no Postgres. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyReportControllerTest extends PostgresIntegrationTest {

    private static final String OVERVIEW = "/api/v1/company/reports/overview";
    private static final String EMPLOYEES = "/api/v1/company/reports/employees";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenProviderPort tokenProviderPort;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> courseIds = new ArrayList<>();
    private int moduleOrder;
    private UUID companyA;
    private UUID companyB;

    @BeforeEach
    void setUp() {
        companyA = givenCompany("Pousada Vista Mar");
        companyB = givenCompany("Hotel Atlântico");
    }

    // O banco é compartilhado entre as classes de teste: cada uma remove o que criou.
    @AfterEach
    void cleanUp() {
        for (final UUID userId : userIds) {
            jdbcTemplate.update("delete from user_courses where user_id = ?", userId);
            jdbcTemplate.update("delete from company_courses where company_id = ?", userId);
            jdbcTemplate.update("delete from users_company where user_id = ? or company_id = ?", userId, userId);
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
        for (final UUID courseId : courseIds) {
            jdbcTemplate.update("delete from courses where id = ?", courseId);
        }
    }

    private UUID givenUser(String name, String role, String status) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into users (id, name, email, password_hash, role, status) "
                + "values (?, ?, ?, 'hash', ?, ?)", id, name, id + "@email.com", role, status);
        userIds.add(id);
        return id;
    }

    private UUID givenCompany(String name) {
        final UUID id = givenUser(name, "COMPANY", "ACTIVE");
        jdbcTemplate.update("insert into company (id, company_name, seats_total) values (?, ?, 10)", id, name);
        return id;
    }

    private UUID givenEmployee(UUID company, String name, String jobTitle, String status) {
        final UUID id = givenUser(name, "STUDENT", status);
        jdbcTemplate.update("insert into users_company (user_id, company_id, role) values (?, ?, ?)",
                id, company, jobTitle);
        return id;
    }

    private UUID givenCourse(String title) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into courses (id, title, status) values (?, ?, 'PUBLISHED')", id, title);
        courseIds.add(id);
        return id;
    }

    private void ownCourse(UUID company, UUID course) {
        jdbcTemplate.update("insert into company_courses (company_id, course_id, data_inicio) values (?, ?, ?)",
                company, course, LocalDate.now());
    }

    private UUID lesson(UUID course, int minutes) {
        final UUID module = UUID.randomUUID();
        moduleOrder++;
        jdbcTemplate.update("insert into modules (id, course_id, title, \"order\") values (?, ?, 'Module', ?)",
                module, course, moduleOrder);
        final UUID lesson = UUID.randomUUID();
        jdbcTemplate.update("insert into content (id, module_id, title, type, duration_minutes, \"order\") "
                + "values (?, ?, 'Lesson', 'VIDEO', ?, 1)", lesson, module, minutes);
        return lesson;
    }

    private void enroll(UUID user, UUID course, int progress, LocalDate conclusion, LocalDateTime lastAccess) {
        jdbcTemplate.update("insert into user_courses (user_id, course_id, dt_inicio, progress, conclusion_date, "
                + "last_access_date) values (?, ?, ?, ?, ?, ?)",
                user, course, LocalDate.now().minusMonths(2), progress, conclusion, lastAccess);
    }

    /** Ana (RECEPCAO) concluiu o curso de 60 min agora e tem certificado; Bruno (COZINHA) nunca acessou. */
    private UUID givenTeamWithOneCompletedCourse(UUID company) {
        final UUID course = givenCourse("IA");
        ownCourse(company, course);
        final UUID lesson = lesson(course, 60);
        final UUID ana = givenEmployee(company, "Ana", "RECEPCAO", "ACTIVE");
        final UUID bruno = givenEmployee(company, "Bruno", "COZINHA", "ACTIVE");
        final LocalDateTime done = LocalDateTime.now().minusMinutes(2);
        enroll(ana, course, 100, LocalDate.now(), done);
        enroll(bruno, course, 0, null, null);
        jdbcTemplate.update("insert into user_content_progress (user_id, content_id, completed_at) values (?, ?, ?)",
                ana, lesson, done);
        jdbcTemplate.update("update user_courses set certificate_issued = true, certificate_issued_at = ? "
                + "where user_id = ?", done, ana);
        return ana;
    }

    private ResultActions getAs(UUID userId, String role, String url) throws Exception {
        final String token = tokenProviderPort.generateToken(userId, userId + "@email.com", role, role);
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    /** A busca vai como parâmetro, e não na URL, para o MockMvc não codificar de novo um % ou um NUL. */
    private ResultActions searchAs(UUID userId, String search) throws Exception {
        final String token = tokenProviderPort.generateToken(userId, userId + "@email.com", "COMPANY", "COMPANY");
        return mockMvc.perform(get(EMPLOYEES).param("search", search).header("Authorization", "Bearer " + token));
    }

    @Test
    void overviewOfACompanyWithoutEmployeesIsZeroedWithSixMonths() throws Exception {
        getAs(companyA, "COMPANY", OVERVIEW)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.completionRate", is(0.0)))
                .andExpect(jsonPath("$.data.completionRateChange", is(0.0)))
                .andExpect(jsonPath("$.data.averageHoursPerEmployee", is(0.0)))
                .andExpect(jsonPath("$.data.certificatesThisQuarter", is(0)))
                .andExpect(jsonPath("$.data.weeklyEngagement", is(0.0)))
                .andExpect(jsonPath("$.data.completionByJobTitle", empty()))
                .andExpect(jsonPath("$.data.monthlyHours", hasSize(6)))
                .andExpect(jsonPath("$.data.monthlyHours[?(@.averageHours > 0)]", empty()));
    }

    @Test
    void overviewReportsEachIndicatorOfTheTeam() throws Exception {
        givenTeamWithOneCompletedCourse(companyA);

        getAs(companyA, "COMPANY", OVERVIEW)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completionRate", is(50.0)))
                .andExpect(jsonPath("$.data.completionRateChange", is(50.0)))
                .andExpect(jsonPath("$.data.averageHoursPerEmployee", is(0.5)))
                .andExpect(jsonPath("$.data.certificatesThisQuarter", is(1)))
                .andExpect(jsonPath("$.data.weeklyEngagement", is(50.0)))
                .andExpect(jsonPath("$.data.completionByJobTitle[*].jobTitle", contains("COZINHA", "RECEPCAO")))
                .andExpect(jsonPath("$.data.completionByJobTitle[*].completionRate", contains(0.0, 100.0)))
                .andExpect(jsonPath("$.data.monthlyHours", hasSize(6)))
                .andExpect(jsonPath("$.data.monthlyHours[?(@.averageHours > 0)]", hasSize(1)))
                .andExpect(jsonPath("$.data.monthlyHours[?(@.averageHours > 0)].averageHours", contains(0.5)));
    }

    @Test
    void overviewNeverMixesInAnotherCompanyOrAClientSuppliedCompanyId() throws Exception {
        givenTeamWithOneCompletedCourse(companyB);

        getAs(companyA, "COMPANY", OVERVIEW + "?companyId=" + companyB)
                .andExpect(jsonPath("$.data.completionRate", is(0.0)))
                .andExpect(jsonPath("$.data.completionByJobTitle", empty()));
        getAs(companyB, "COMPANY", OVERVIEW).andExpect(jsonPath("$.data.completionRate", is(50.0)));
    }

    @Test
    void employeesListsOneRowPerEmployeeAndCourseByMostRecentAccess() throws Exception {
        final UUID ana = givenTeamWithOneCompletedCourse(companyA);

        getAs(companyA, "COMPANY", EMPLOYEES)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content[0].userId", is(ana.toString())))
                .andExpect(jsonPath("$.data.content[0].name", is("Ana")))
                .andExpect(jsonPath("$.data.content[0].course", is("IA")))
                .andExpect(jsonPath("$.data.content[0].progress", is(100)))
                .andExpect(jsonPath("$.data.content[0].certificateIssued", is(true)))
                .andExpect(jsonPath("$.data.content[1].name", is("Bruno")))
                .andExpect(jsonPath("$.data.content[1].progress", is(0)))
                .andExpect(jsonPath("$.data.content[1].lastAccessAt", nullValue()))
                .andExpect(jsonPath("$.data.content[1].certificateIssued", is(false)));
    }

    @Test
    void employeesFiltersByStatusAndSearchesByNameOfTheCompanyOnly() throws Exception {
        givenTeamWithOneCompletedCourse(companyA);
        givenTeamWithOneCompletedCourse(companyB);

        getAs(companyA, "COMPANY", EMPLOYEES + "?status=COMPLETED")
                .andExpect(jsonPath("$.data.content[*].name", contains("Ana")));
        getAs(companyA, "COMPANY", EMPLOYEES + "?status=IN_PROGRESS")
                .andExpect(jsonPath("$.data.content[*].name", contains("Bruno")));
        getAs(companyA, "COMPANY", EMPLOYEES + "?search=BRU")
                .andExpect(jsonPath("$.data.content[*].name", contains("Bruno")))
                .andExpect(jsonPath("$.data.totalElements", is(1)));
        getAs(companyA, "COMPANY", EMPLOYEES + "?companyId=" + companyB)
                .andExpect(jsonPath("$.data.totalElements", is(2)));
    }

    @Test
    void employeesPaginates() throws Exception {
        givenTeamWithOneCompletedCourse(companyA);

        getAs(companyA, "COMPANY", EMPLOYEES + "?page=1&size=1")
                .andExpect(jsonPath("$.data.content[*].name", contains("Bruno")))
                .andExpect(jsonPath("$.data.pageNumber", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(1)))
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.totalPages", is(2)));
    }

    @Test
    void employeesRefusesInvalidParameters() throws Exception {
        getAs(companyA, "COMPANY", EMPLOYEES + "?status=banana").andExpect(status().isBadRequest());
        getAs(companyA, "COMPANY", EMPLOYEES + "?page=-1").andExpect(status().isBadRequest());
        getAs(companyA, "COMPANY", EMPLOYEES + "?size=0").andExpect(status().isBadRequest());
        searchAs(companyA, "a\u0000b")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid search")));
    }

    @Test
    void returnsNotFoundForACompanyUserThatHasNoCompanyRecord() throws Exception {
        final UUID onlyInUsers = givenUser("Sem Registro", "COMPANY", "ACTIVE");

        getAs(onlyInUsers, "COMPANY", OVERVIEW)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Company not found: " + onlyInUsers)));
        getAs(onlyInUsers, "COMPANY", EMPLOYEES).andExpect(status().isNotFound());
    }

    @Test
    void returnsUnauthorizedWithoutATokenAndForbiddenForAnyProfileThatIsNotACompany() throws Exception {
        final UUID student = givenUser("Aluno", "STUDENT", "ACTIVE");
        final UUID admin = givenUser("Admin", "ADMIN", "ACTIVE");

        mockMvc.perform(get(OVERVIEW)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EMPLOYEES)).andExpect(status().isUnauthorized());
        getAs(student, "STUDENT", OVERVIEW).andExpect(status().isForbidden());
        getAs(student, "STUDENT", EMPLOYEES).andExpect(status().isForbidden());
        getAs(admin, "ADMIN", OVERVIEW).andExpect(status().isForbidden());
        getAs(admin, "ADMIN", EMPLOYEES).andExpect(status().isForbidden());
    }
}
