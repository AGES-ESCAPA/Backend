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

import java.sql.Timestamp;
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

/** GET /api/v1/company/seats e /employees de ponta a ponta, com a consulta SQL real no Postgres. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyEmployeeControllerTest extends PostgresIntegrationTest {

    private static final String SEATS = "/api/v1/company/seats";
    private static final String EMPLOYEES = "/api/v1/company/employees";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    // Empresas, colaboradores e cursos de teste são criados direto nas tabelas.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> courseIds = new ArrayList<>();

    private UUID companyA;
    private UUID companyB;

    @BeforeEach
    void setUp() {
        companyA = givenCompany("Pousada Vista Mar", 10);
        companyB = givenCompany("Hotel Atlântico", 3);
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

    private UUID givenUser(String name, String email, String role, String status, String avatarUrl) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role, status, avatar_url)"
                        + " values (?, ?, ?, 'hash', ?, ?, ?)",
                id, name, email, role, status, avatarUrl);
        userIds.add(id);
        return id;
    }

    private UUID givenCompany(String name, int seatsTotal) {
        final UUID id = givenUser(name, name.toLowerCase().replace(' ', '.') + "-" + UUID.randomUUID() + "@empresa.com.br",
                "COMPANY", "ACTIVE", null);
        jdbcTemplate.update("insert into company (id, company_name, seats_total) values (?, ?, ?)", id, name, seatsTotal);
        return id;
    }

    private UUID givenEmployee(UUID company, String name, String email, String jobTitle, String status) {
        return givenEmployee(company, name, email, jobTitle, status, null);
    }

    private UUID givenEmployee(UUID company, String name, String email, String jobTitle, String status, String avatar) {
        final UUID id = givenUser(name, email, "STUDENT", status, avatar);
        jdbcTemplate.update("insert into users_company (user_id, company_id, role) values (?, ?, ?)", id, company, jobTitle);
        return id;
    }

    private UUID givenCourse() {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into courses (id, title, status) values (?, 'Curso', 'PUBLISHED')", id);
        courseIds.add(id);
        return id;
    }

    private void ownCourse(UUID company, UUID course, boolean revoked) {
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio, revoked_at) values (?, ?, ?, ?)",
                company, course, java.sql.Date.valueOf(LocalDate.now()),
                revoked ? Timestamp.valueOf(LocalDateTime.now()) : null);
    }

    private void enroll(UUID user, UUID course, boolean revoked) {
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, progress, revoked_at) values (?, ?, 0, ?)",
                user, course, revoked ? Timestamp.valueOf(LocalDateTime.now()) : null);
    }

    private String bearer(UUID userId, String role) {
        return "Bearer " + tokenProviderPort.generateToken(userId, userId + "@email.com", role, role);
    }

    private ResultActions getAs(UUID userId, String role, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer(userId, role)));
    }

    /** A busca vai como parâmetro, e não na URL, para o MockMvc não codificar de novo um % ou um espaço. */
    private ResultActions searchAs(UUID userId, String search) throws Exception {
        return mockMvc.perform(get(EMPLOYEES).param("search", search).header("Authorization", bearer(userId, "COMPANY")));
    }

    /** Dez vagas e nove colaboradores: 6 ativos, 2 pendentes e 1 inativo, como no exemplo da tarefa. */
    private void givenNineEmployeesInCompanyA() {
        for (int index = 1; index <= 6; index++) {
            givenEmployee(companyA, "Ativo " + index, "ativo" + index + "@vistamar.com.br", "Recepção", "ACTIVE");
        }
        givenEmployee(companyA, "Pendente 1", "pendente1@vistamar.com.br", "Cozinha", "PENDING");
        givenEmployee(companyA, "Pendente 2", "pendente2@vistamar.com.br", "Cozinha", "PENDING");
        givenEmployee(companyA, "Inativo 1", "inativo1@vistamar.com.br", "Manutenção", "INACTIVE");
    }

    @Test
    void shouldSummarizeTheSeatsCountingEmployeesOfAnyStatus() throws Exception {
        givenNineEmployeesInCompanyA();

        getAs(companyA, "COMPANY", SEATS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.seatsTotal", is(10)))
                .andExpect(jsonPath("$.data.seatsUsed", is(9)))
                .andExpect(jsonPath("$.data.seatsAvailable", is(1)))
                .andExpect(jsonPath("$.data.employeesTotal", is(9)))
                .andExpect(jsonPath("$.data.employeesActive", is(6)));
    }

    @Test
    void shouldKeepASeatTakenByAPendingOrInactiveEmployee() throws Exception {
        givenEmployee(companyA, "Pendente", "pendente@vistamar.com.br", "Recepção", "PENDING");
        givenEmployee(companyA, "Inativo", "inativo@vistamar.com.br", "Recepção", "INACTIVE");

        getAs(companyA, "COMPANY", SEATS)
                .andExpect(jsonPath("$.data.seatsUsed", is(2)))
                .andExpect(jsonPath("$.data.seatsAvailable", is(8)))
                .andExpect(jsonPath("$.data.employeesTotal", is(2)))
                .andExpect(jsonPath("$.data.employeesActive", is(0)));
    }

    @Test
    void shouldReportEveryContractedSeatFreeForACompanyWithoutEmployees() throws Exception {
        getAs(companyA, "COMPANY", SEATS)
                .andExpect(jsonPath("$.data.seatsTotal", is(10)))
                .andExpect(jsonPath("$.data.seatsUsed", is(0)))
                .andExpect(jsonPath("$.data.seatsAvailable", is(10)))
                .andExpect(jsonPath("$.data.employeesTotal", is(0)))
                .andExpect(jsonPath("$.data.employeesActive", is(0)));
    }

    @Test
    void shouldNeverReportNegativeAvailableSeats() throws Exception {
        for (int index = 1; index <= 4; index++) {
            givenEmployee(companyB, "Colab " + index, "colab" + index + "@atlantico.com.br", "Recepção", "ACTIVE");
        }

        getAs(companyB, "COMPANY", SEATS)
                .andExpect(jsonPath("$.data.seatsTotal", is(3)))
                .andExpect(jsonPath("$.data.seatsUsed", is(4)))
                .andExpect(jsonPath("$.data.seatsAvailable", is(0)));
    }

    @Test
    void shouldSeparateTheSeatsOfEachCompany() throws Exception {
        givenNineEmployeesInCompanyA();
        givenEmployee(companyB, "Colab B", "colab.b@atlantico.com.br", "Recepção", "ACTIVE");

        getAs(companyA, "COMPANY", SEATS).andExpect(jsonPath("$.data.seatsUsed", is(9)));
        getAs(companyB, "COMPANY", SEATS)
                .andExpect(jsonPath("$.data.seatsTotal", is(3)))
                .andExpect(jsonPath("$.data.seatsUsed", is(1)))
                .andExpect(jsonPath("$.data.seatsAvailable", is(2)))
                .andExpect(jsonPath("$.data.employeesActive", is(1)));
    }

    @Test
    void shouldListOnlyTheEmployeesOfTheCompanyOfTheTokenOrderedByName() throws Exception {
        givenEmployee(companyA, "Carla Souza", "carla@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "ana paula ferreira", "ana@vistamar.com.br", "Governança", "ACTIVE");
        givenEmployee(companyA, "Bruno Alves", "bruno@vistamar.com.br", "Cozinha", "PENDING");
        givenEmployee(companyB, "Aline da Outra Empresa", "aline@atlantico.com.br", "Recepção", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[*].name", contains("ana paula ferreira", "Bruno Alves", "Carla Souza")))
                .andExpect(jsonPath("$.data.totalElements", is(3)));

        getAs(companyB, "COMPANY", EMPLOYEES)
                .andExpect(jsonPath("$.data.content[*].name", contains("Aline da Outra Empresa")));
    }

    @Test
    void shouldOrderNamesWithAccentsAmongTheirPlainLetters() throws Exception {
        givenEmployee(companyA, "Zeca Lima", "zeca@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Érica Dias", "erica@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Ana Costa", "ana@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Ágata Reis", "agata@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Bruno Alves", "bruno@vistamar.com.br", "Recepção", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES)
                .andExpect(jsonPath("$.data.content[*].name",
                        contains("Ágata Reis", "Ana Costa", "Bruno Alves", "Érica Dias", "Zeca Lima")));
    }

    @Test
    void shouldFindANameWithAccentsIgnoringCase() throws Exception {
        givenEmployee(companyA, "Érica Dias", "erica@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Bruno Alves", "bruno@vistamar.com.br", "Recepção", "ACTIVE");

        searchAs(companyA, "érica").andExpect(jsonPath("$.data.content[*].name", contains("Érica Dias")));
        searchAs(companyA, "ÉRICA").andExpect(jsonPath("$.data.content[*].name", contains("Érica Dias")));
    }

    @Test
    void shouldReturnTheFieldsOfEachEmployee() throws Exception {
        final String avatar = "http://localhost:9000/escapa-media/avatars/ana.png";
        final UUID ana = givenEmployee(
                companyA, "Ana Paula Ferreira", "ana.ferreira@empresa.com.br", "Recepção", "ACTIVE", avatar);
        givenEmployee(companyA, "Bruno Alves", "bruno@empresa.com.br", "Cozinha", "PENDING");

        getAs(companyA, "COMPANY", EMPLOYEES)
                .andExpect(jsonPath("$.data.content[0].id", is(ana.toString())))
                .andExpect(jsonPath("$.data.content[0].name", is("Ana Paula Ferreira")))
                .andExpect(jsonPath("$.data.content[0].email", is("ana.ferreira@empresa.com.br")))
                .andExpect(jsonPath("$.data.content[0].jobTitle", is("Recepção")))
                .andExpect(jsonPath("$.data.content[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.content[0].assignedCourses", is(0)))
                .andExpect(jsonPath("$.data.content[0].avatarUrl", is(avatar)))
                .andExpect(jsonPath("$.data.content[1].status", is("PENDING")))
                .andExpect(jsonPath("$.data.content[1].avatarUrl", nullValue()));
    }

    @Test
    void shouldShowEmployeesOfEveryStatus() throws Exception {
        givenNineEmployeesInCompanyA();

        getAs(companyA, "COMPANY", EMPLOYEES + "?size=20")
                .andExpect(jsonPath("$.data.content", hasSize(9)))
                .andExpect(jsonPath("$.data.content[?(@.status == 'ACTIVE')]", hasSize(6)))
                .andExpect(jsonPath("$.data.content[?(@.status == 'PENDING')]", hasSize(2)))
                .andExpect(jsonPath("$.data.content[?(@.status == 'INACTIVE')]", hasSize(1)));
    }

    @Test
    void shouldCountOnlyTheEnrollmentsThatWereNotRevokedInTheCoursesOfTheCompany() throws Exception {
        final UUID employee = givenEmployee(companyA, "Ana", "ana@vistamar.com.br", "Recepção", "ACTIVE");
        final UUID owned1 = givenCourse();
        final UUID owned2 = givenCourse();
        final UUID ownedButRevoked = givenCourse();
        final UUID notOwnedByTheCompany = givenCourse();
        final UUID ownedByAnotherCompany = givenCourse();
        ownCourse(companyA, owned1, false);
        ownCourse(companyA, owned2, false);
        ownCourse(companyA, ownedButRevoked, true);
        ownCourse(companyB, ownedByAnotherCompany, false);
        enroll(employee, owned1, false);
        enroll(employee, owned2, false);
        enroll(employee, ownedButRevoked, false);
        enroll(employee, notOwnedByTheCompany, false);
        enroll(employee, ownedByAnotherCompany, false);
        final UUID revokedEnrollmentCourse = givenCourse();
        ownCourse(companyA, revokedEnrollmentCourse, false);
        enroll(employee, revokedEnrollmentCourse, true);

        getAs(companyA, "COMPANY", EMPLOYEES)
                .andExpect(jsonPath("$.data.content[0].assignedCourses", is(2)));
    }

    @Test
    void shouldCountTheCoursesOfEachEmployeeSeparately() throws Exception {
        final UUID ana = givenEmployee(companyA, "Ana", "ana@vistamar.com.br", "Recepção", "ACTIVE");
        final UUID bruno = givenEmployee(companyA, "Bruno", "bruno@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Carla", "carla@vistamar.com.br", "Recepção", "ACTIVE");
        final UUID course1 = givenCourse();
        final UUID course2 = givenCourse();
        ownCourse(companyA, course1, false);
        ownCourse(companyA, course2, false);
        enroll(ana, course1, false);
        enroll(ana, course2, false);
        enroll(bruno, course1, false);

        getAs(companyA, "COMPANY", EMPLOYEES)
                .andExpect(jsonPath("$.data.content[*].assignedCourses", contains(2, 1, 0)));
    }

    @Test
    void shouldPaginateTheEmployees() throws Exception {
        for (final String name : List.of("Ana", "Bruno", "Carla", "Diego", "Elisa")) {
            givenEmployee(companyA, name, name.toLowerCase() + "@vistamar.com.br", "Recepção", "ACTIVE");
        }

        getAs(companyA, "COMPANY", EMPLOYEES + "?page=0&size=2")
                .andExpect(jsonPath("$.data.content[*].name", contains("Ana", "Bruno")))
                .andExpect(jsonPath("$.data.pageNumber", is(0)))
                .andExpect(jsonPath("$.data.pageSize", is(2)))
                .andExpect(jsonPath("$.data.totalElements", is(5)))
                .andExpect(jsonPath("$.data.totalPages", is(3)));
        getAs(companyA, "COMPANY", EMPLOYEES + "?page=2&size=2")
                .andExpect(jsonPath("$.data.content[*].name", contains("Elisa")));
        getAs(companyA, "COMPANY", EMPLOYEES + "?page=5&size=2")
                .andExpect(jsonPath("$.data.content", empty()))
                .andExpect(jsonPath("$.data.totalElements", is(5)));
    }

    @Test
    void shouldUseTheFirstPageOfTenByDefault() throws Exception {
        for (int index = 1; index <= 12; index++) {
            givenEmployee(companyA, String.format("Colab %02d", index), "colab" + index + "@vistamar.com.br",
                    "Recepção", "ACTIVE");
        }

        getAs(companyA, "COMPANY", EMPLOYEES)
                .andExpect(jsonPath("$.data.content", hasSize(10)))
                .andExpect(jsonPath("$.data.pageNumber", is(0)))
                .andExpect(jsonPath("$.data.pageSize", is(10)))
                .andExpect(jsonPath("$.data.totalElements", is(12)))
                .andExpect(jsonPath("$.data.totalPages", is(2)));
    }

    @Test
    void shouldFindAnEmployeeByNameIgnoringCase() throws Exception {
        givenEmployee(companyA, "Ana Paula Ferreira", "ana.ferreira@empresa.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Bruno Alves", "bruno@empresa.com.br", "Cozinha", "ACTIVE");
        givenEmployee(companyA, "Joana Dias", "joana@empresa.com.br", "Cozinha", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES + "?search=ANA&page=0&size=10")
                .andExpect(jsonPath("$.data.content[*].name", contains("Ana Paula Ferreira", "Joana Dias")))
                .andExpect(jsonPath("$.data.totalElements", is(2)));
    }

    @Test
    void shouldFindAnEmployeeByEmailIgnoringCase() throws Exception {
        givenEmployee(companyA, "Bruno Alves", "BRUNO.ALVES@Empresa.com.br", "Cozinha", "ACTIVE");
        givenEmployee(companyA, "Carla Souza", "carla@outra.com.br", "Cozinha", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES + "?search=bruno.alves@empresa")
                .andExpect(jsonPath("$.data.content[*].name", contains("Bruno Alves")));
        getAs(companyA, "COMPANY", EMPLOYEES + "?search=OUTRA.COM")
                .andExpect(jsonPath("$.data.content[*].name", contains("Carla Souza")));
    }

    @Test
    void shouldPaginateTheSearchResults() throws Exception {
        for (int index = 1; index <= 5; index++) {
            givenEmployee(companyA, "Ana " + index, "ana" + index + "@vistamar.com.br", "Recepção", "ACTIVE");
        }
        givenEmployee(companyA, "Bruno", "bruno@vistamar.com.br", "Recepção", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES + "?search=ana&page=1&size=2")
                .andExpect(jsonPath("$.data.content[*].name", contains("Ana 3", "Ana 4")))
                .andExpect(jsonPath("$.data.totalElements", is(5)))
                .andExpect(jsonPath("$.data.totalPages", is(3)));
    }

    @Test
    void shouldReturnEverythingWhenTheSearchIsBlank() throws Exception {
        givenEmployee(companyA, "Ana", "ana@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Bruno", "bruno@vistamar.com.br", "Recepção", "ACTIVE");

        searchAs(companyA, "")
                .andExpect(jsonPath("$.data.content", hasSize(2)));
        searchAs(companyA, "  ")
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void shouldReturnAnEmptyListWhenNobodyMatchesTheSearch() throws Exception {
        givenEmployee(companyA, "Ana", "ana@vistamar.com.br", "Recepção", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES + "?search=zzz")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()))
                .andExpect(jsonPath("$.data.totalElements", is(0)))
                .andExpect(jsonPath("$.data.totalPages", is(0)));
    }

    @Test
    void shouldTreatPercentAndUnderscoreAsPlainTextInTheSearch() throws Exception {
        givenEmployee(companyA, "Ana 50% Desconto", "ana50@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Bruno Alves", "bruno@vistamar.com.br", "Recepção", "ACTIVE");
        givenEmployee(companyA, "Carla_Souza", "carla@vistamar.com.br", "Recepção", "ACTIVE");

        searchAs(companyA, "%")
                .andExpect(jsonPath("$.data.content[*].name", contains("Ana 50% Desconto")));
        searchAs(companyA, "_")
                .andExpect(jsonPath("$.data.content[*].name", contains("Carla_Souza")));
    }

    @Test
    void shouldNotRevealAnEmployeeOfAnotherCompanyThroughTheSearch() throws Exception {
        givenEmployee(companyB, "Aline Secreta", "aline.secreta@atlantico.com.br", "Recepção", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES + "?search=aline")
                .andExpect(jsonPath("$.data.content", empty()));
        getAs(companyA, "COMPANY", EMPLOYEES + "?search=atlantico")
                .andExpect(jsonPath("$.data.content", empty()));
    }

    @Test
    void shouldRefuseAnInvalidPagination() throws Exception {
        getAs(companyA, "COMPANY", EMPLOYEES + "?page=-1").andExpect(status().isBadRequest());
        getAs(companyA, "COMPANY", EMPLOYEES + "?size=0").andExpect(status().isBadRequest());
        getAs(companyA, "COMPANY", EMPLOYEES + "?size=101").andExpect(status().isBadRequest());
        getAs(companyA, "COMPANY", EMPLOYEES + "?size=abc").andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWithoutAToken() throws Exception {
        mockMvc.perform(get(SEATS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EMPLOYEES)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenForAnyProfileThatIsNotACompany() throws Exception {
        final UUID student = givenUser("Aluno", "aluno-" + UUID.randomUUID() + "@email.com", "STUDENT", "ACTIVE", null);
        final UUID admin = givenUser("Admin", "admin-" + UUID.randomUUID() + "@email.com", "ADMIN", "ACTIVE", null);

        getAs(student, "STUDENT", SEATS).andExpect(status().isForbidden());
        getAs(student, "STUDENT", EMPLOYEES).andExpect(status().isForbidden());
        getAs(admin, "ADMIN", SEATS).andExpect(status().isForbidden());
        getAs(admin, "ADMIN", EMPLOYEES).andExpect(status().isForbidden());
    }

    @Test
    void shouldIgnoreAnyCompanyIdSentByTheClient() throws Exception {
        givenEmployee(companyB, "Aline da Outra", "aline@atlantico.com.br", "Recepção", "ACTIVE");

        getAs(companyA, "COMPANY", EMPLOYEES + "?companyId=" + companyB)
                .andExpect(jsonPath("$.data.content", empty()));
        getAs(companyA, "COMPANY", SEATS + "?companyId=" + companyB)
                .andExpect(jsonPath("$.data.seatsTotal", is(10)));
    }
}
