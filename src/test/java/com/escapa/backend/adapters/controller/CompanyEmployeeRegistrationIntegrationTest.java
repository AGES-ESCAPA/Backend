package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.port.EmailSenderPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/v1/company/employees e /{id}/invite de ponta a ponta, com o Postgres real. O envio de
 * e-mail é trocado por um registro em memória: o SMTP em si já tem teste próprio com o Mailpit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyEmployeeRegistrationIntegrationTest extends PostgresIntegrationTest {

    private static final String EMPLOYEES = "/api/v1/company/employees";
    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/criar-senha\\?token=([A-Za-z0-9_-]+)");

    /** Guarda os e-mails que sairiam; {@code failNext} simula o servidor de e-mail fora do ar. */
    static final class RecordingEmailSender implements EmailSenderPort {

        record Sent(String to, String subject, String textBody) {
        }

        final List<Sent> sent = new CopyOnWriteArrayList<>();
        volatile boolean failNext;

        @Override
        public void send(String to, String subject, String htmlBody, String textBody) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("smtp down");
            }
            sent.add(new Sent(to, subject, textBody));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmailTestConfig {

        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingEmailSender emailSender;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> courseIds = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();

    private UUID company;
    private UUID otherCompany;
    private UUID courseWithDeadline;
    private UUID courseWithoutDeadline;

    @BeforeEach
    void setUp() {
        emailSender.sent.clear();
        emailSender.failNext = false;
        company = givenCompany("Pousada Vista Mar", 3);
        otherCompany = givenCompany("Hotel Atlântico", 3);
        courseWithDeadline = givenCourse(30);
        courseWithoutDeadline = givenCourse(null);
        final UUID revokedCourse = givenCourse(10);
        ownCourse(company, courseWithDeadline, false);
        ownCourse(company, courseWithoutDeadline, false);
        ownCourse(company, revokedCourse, true);
    }

    @AfterEach
    void cleanUp() {
        // user_courses, users_company e account_invites saem junto pelo ON DELETE CASCADE.
        for (final String email : emails) {
            jdbcTemplate.update("delete from users where email = ?", email);
        }
        for (final UUID userId : userIds) {
            jdbcTemplate.update("delete from company_courses where company_id = ?", userId);
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
        for (final UUID courseId : courseIds) {
            jdbcTemplate.update("delete from courses where id = ?", courseId);
        }
    }

    @Test
    void shouldRegisterAPendingEmployeeEnrollInTheCompanyCoursesAndEmailTheInvite() throws Exception {
        final String email = newEmail("joao");

        registerAs(company, "João da Silva", email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("João da Silva")))
                .andExpect(jsonPath("$.data.email", is(email)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.assignedCourses", is(2)))
                .andExpect(jsonPath("$.data.avatarUrl", nullValue()))
                .andExpect(jsonPath("$.data.inviteSent", is(true)));

        final UUID employeeId = userIdOf(email);
        assertEquals("STUDENT", jdbcTemplate.queryForObject("select role from users where id = ?", String.class, employeeId));
        assertEquals("PENDING", jdbcTemplate.queryForObject("select status from users where id = ?", String.class, employeeId));
        assertEquals(1, count("users_company where user_id = ? and company_id = ?", employeeId, company));
        assertEquals(2, count("user_courses where user_id = ? and revoked_at is null", employeeId));
        assertEnrollment(employeeId, courseWithDeadline, LocalDate.now().plusDays(30));
        assertEnrollment(employeeId, courseWithoutDeadline, null);
        assertEquals(1, count("account_invites where user_id = ? and used_at is null", employeeId));
    }

    @Test
    void shouldSendTheEmailWithATokenThatMatchesTheStoredInviteAndLetTheEmployeeCreateThePassword() throws Exception {
        final String email = newEmail("maria");
        registerAs(company, "Maria", email).andExpect(status().isCreated());

        assertEquals(1, emailSender.sent.size());
        final RecordingEmailSender.Sent sent = emailSender.sent.get(0);
        assertEquals(email, sent.to());
        final String token = tokenIn(sent.textBody());
        assertEquals(1, count("account_invites where user_id = ? and token_hash = ?",
                userIdOf(email), AccountInvite.hashToken(token)));

        mockMvc.perform(post("/api/v1/auth/invites/" + token + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"senha-segura\"}"))
                .andExpect(status().isOk());
        assertEquals("ACTIVE", jdbcTemplate.queryForObject(
                "select status from users where id = ?", String.class, userIdOf(email)));
    }

    @Test
    void shouldKeepTheRegistrationWhenTheEmailFailsAndReportItInTheResponse() throws Exception {
        final String email = newEmail("ana");
        emailSender.failNext = true;

        registerAs(company, "Ana", email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.inviteSent", is(false)));

        assertEquals(1, count("users_company where user_id = ?", userIdOf(email)));
        assertEquals(1, count("account_invites where user_id = ?", userIdOf(email)));
        assertTrue(emailSender.sent.isEmpty());
    }

    @Test
    void shouldRefuseWithConflictAndCreateNothingWhenThereIsNoFreeSeat() throws Exception {
        for (int index = 0; index < 3; index++) {
            registerAs(company, "Colaborador " + index, newEmail("c" + index)).andExpect(status().isCreated());
        }
        final String email = newEmail("sem-vaga");

        registerAs(company, "Sem Vaga", email).andExpect(status().isConflict());

        assertEquals(0, count("users where email = ?", email));
        assertEquals(3, count("users_company where company_id = ?", company));
    }

    @Test
    void shouldRefuseWithConflictAnEmailThatIsAlreadyRegistered() throws Exception {
        final String email = newEmail("repetido");
        registerAs(company, "Primeiro", email).andExpect(status().isCreated());

        registerAs(otherCompany, "Segundo", email.toUpperCase()).andExpect(status().isConflict());

        assertEquals(1, count("users where email = ?", email));
        assertEquals(0, count("users_company where company_id = ?", otherCompany));
    }

    @Test
    void shouldReactivateAFormerEmployeeKeepingTheProgress() throws Exception {
        final String email = newEmail("ex");
        final UUID formerId = givenFormerEmployee(email);
        jdbcTemplate.update("insert into user_courses (user_id, course_id, progress, revoked_at, dt_inicio)"
                        + " values (?, ?, 40, ?, ?)",
                formerId, courseWithDeadline, Timestamp.valueOf(LocalDateTime.now().minusDays(5)),
                Date.valueOf(LocalDate.now().minusDays(60)));

        registerAs(company, "Ex Colaborador", email).andExpect(status().isCreated());

        assertEquals(formerId, userIdOf(email));
        assertEquals(1, count("users where email = ?", email));
        assertEquals("PENDING", jdbcTemplate.queryForObject("select status from users where id = ?", String.class, formerId));
        assertEquals(40, jdbcTemplate.queryForObject(
                "select progress from user_courses where user_id = ? and course_id = ?",
                Integer.class, formerId, courseWithDeadline));
        assertEquals(2, count("user_courses where user_id = ? and revoked_at is null", formerId));
    }

    @Test
    void shouldRejectInvalidData() throws Exception {
        registerAs(company, "", newEmail("a")).andExpect(status().isBadRequest());
        registerAs(company, "João", "sem-arroba").andExpect(status().isBadRequest());
        mockMvc.perform(post(EMPLOYEES).header("Authorization", bearer(company, "COMPANY"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldResendTheInviteInvalidatingThePreviousLink() throws Exception {
        final String email = newEmail("reenvio");
        registerAs(company, "Reenvio", email).andExpect(status().isCreated());
        final String oldToken = tokenIn(emailSender.sent.get(0).textBody());
        final UUID employeeId = userIdOf(email);

        resendAs(company, employeeId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteSent", is(true)));

        assertEquals(2, emailSender.sent.size());
        final String newToken = tokenIn(emailSender.sent.get(1).textBody());
        assertNotEquals(oldToken, newToken);
        assertEquals(1, count("account_invites where user_id = ? and used_at is null", employeeId));
        mockMvc.perform(get("/api/v1/auth/invites/" + oldToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/auth/invites/" + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRefuseToResendToAnEmployeeThatIsNotPending() throws Exception {
        final String email = newEmail("ativo");
        registerAs(company, "Ativo", email).andExpect(status().isCreated());
        jdbcTemplate.update("update users set status = 'ACTIVE' where email = ?", email);

        resendAs(company, userIdOf(email)).andExpect(status().isConflict());
    }

    @Test
    void shouldAnswerNotFoundWhenResendingToAnEmployeeOfAnotherCompany() throws Exception {
        final String email = newEmail("outra");
        registerAs(company, "Outra", email).andExpect(status().isCreated());

        resendAs(otherCompany, userIdOf(email)).andExpect(status().isNotFound());
        resendAs(company, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireACompanyToken() throws Exception {
        final UUID student = givenUser("Aluno", newEmail("aluno"), "STUDENT", "ACTIVE");
        final String body = "{\"name\":\"João\",\"email\":\"" + newEmail("x") + "\"}";

        mockMvc.perform(post(EMPLOYEES).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(EMPLOYEES).header("Authorization", bearer(student, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(EMPLOYEES + "/" + UUID.randomUUID() + "/invite"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(EMPLOYEES + "/" + UUID.randomUUID() + "/invite")
                        .header("Authorization", bearer(student, "STUDENT")))
                .andExpect(status().isForbidden());
    }

    private ResultActions registerAs(UUID companyId, String name, String email) throws Exception {
        final String body = "{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}";
        return mockMvc.perform(post(EMPLOYEES).header("Authorization", bearer(companyId, "COMPANY"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions resendAs(UUID companyId, UUID employeeId) throws Exception {
        return mockMvc.perform(post(EMPLOYEES + "/" + employeeId + "/invite")
                .header("Authorization", bearer(companyId, "COMPANY")));
    }

    private String bearer(UUID userId, String role) {
        return "Bearer " + tokenProviderPort.generateToken(userId, userId + "@email.com", role, role);
    }

    private static String tokenIn(String textBody) {
        final Matcher matcher = TOKEN_IN_LINK.matcher(textBody);
        assertTrue(matcher.find(), "the e-mail must carry the create-password link");
        return matcher.group(1);
    }

    private String newEmail(String prefix) {
        final String email = prefix + "-" + UUID.randomUUID() + "@empresa.com.br";
        emails.add(email);
        return email;
    }

    private UUID userIdOf(String email) {
        final UUID id = jdbcTemplate.queryForObject("select id from users where email = ?", UUID.class, email);
        assertNotNull(id);
        return id;
    }

    private int count(String fromAndWhere, Object... args) {
        final Integer total = jdbcTemplate.queryForObject("select count(*) from " + fromAndWhere, Integer.class, args);
        return total == null ? 0 : total;
    }

    private void assertEnrollment(UUID userId, UUID courseId, LocalDate expiration) {
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "select dt_inicio, dt_expiracao from user_courses where user_id = ? and course_id = ?", userId, courseId);
        assertEquals(Date.valueOf(LocalDate.now()), row.get("dt_inicio"));
        assertEquals(expiration == null ? null : Date.valueOf(expiration), row.get("dt_expiracao"));
    }

    private UUID givenUser(String name, String email, String role, String status) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into users (id, name, email, password_hash, role, status) values (?, ?, ?, 'hash', ?, ?)",
                id, name, email, role, status);
        userIds.add(id);
        return id;
    }

    private UUID givenCompany(String name, int seatsTotal) {
        final UUID id = givenUser(name, newEmail("empresa"), "COMPANY", "ACTIVE");
        jdbcTemplate.update("insert into company (id, company_name, seats_total) values (?, ?, ?)", id, name, seatsTotal);
        return id;
    }

    private UUID givenCourse(Integer deadline) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into courses (id, title, status, deadline) values (?, 'Curso', 'PUBLISHED', ?)",
                id, deadline);
        courseIds.add(id);
        return id;
    }

    private void ownCourse(UUID companyId, UUID courseId, boolean revoked) {
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio, revoked_at) values (?, ?, ?, ?)",
                companyId, courseId, Date.valueOf(LocalDate.now()),
                revoked ? Timestamp.valueOf(LocalDateTime.now()) : null);
    }

    /** Conta inativa, sem vínculo com empresa, que já recebeu um convite (foi colaborador). */
    private UUID givenFormerEmployee(String email) {
        final UUID id = givenUser("Ex", email, "STUDENT", "INACTIVE");
        jdbcTemplate.update(
                "insert into account_invites (id, user_id, company_id, token_hash, expires_at, used_at)"
                        + " values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), id, otherCompany, "hash-" + UUID.randomUUID(),
                Timestamp.valueOf(LocalDateTime.now().minusDays(30)), Timestamp.valueOf(LocalDateTime.now().minusDays(60)));
        return id;
    }
}
