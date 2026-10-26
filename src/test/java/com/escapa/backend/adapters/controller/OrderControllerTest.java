package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/v1/orders de ponta a ponta: autenticação, pedido, liberação de acesso e erros. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest extends PostgresIntegrationTest {

    private static final String ORDERS = "/api/v1/orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    // Usuários, empresas e cursos de teste são criados direto nas tabelas.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // O banco é compartilhado entre as classes de teste e pedidos não têm ON DELETE CASCADE:
    // sem esta limpeza, outros testes que fazem deleteAll em users/courses quebram.
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from company_courses");
        jdbcTemplate.update("delete from user_courses");
    }

    private UUID givenUser(String role) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role) values (?, ?, ?, 'hash', ?)",
                id, "Comprador", id + "@email.com", role);
        return id;
    }

    private UUID givenCompany() {
        final UUID id = givenUser("COMPANY");
        jdbcTemplate.update("insert into company (id, company_name) values (?, 'Hotel Teste LTDA')", id);
        return id;
    }

    private UUID givenCourse(String status, Double price, Integer deadline) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into courses (id, title, status, price, deadline) values (?, 'Curso Teste', ?, ?, ?)",
                id, status, price, deadline);
        return id;
    }

    private String bearer(UUID userId, String role) {
        return "Bearer " + tokenProviderPort.generateToken(userId, userId + "@email.com", role, role);
    }

    private ResultActions placeOrder(UUID userId, String role, String body) throws Exception {
        return mockMvc.perform(post(ORDERS)
                .header("Authorization", bearer(userId, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String courseBody(UUID courseId, String paymentMethod, int installments) {
        return """
                {"itemType": "COURSE", "courseId": "%s", "paymentMethod": "%s", "installments": %d}
                """.formatted(courseId, paymentMethod, installments);
    }

    private static String seatsBody(int quantity) {
        return """
                {"itemType": "SEATS", "seatsQuantity": %d, "paymentMethod": "CARD", "installments": 1}
                """.formatted(quantity);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Test
    void shouldApproveTheCourseOfAStudentAndEnrollThemFromTodayUntilTheDeadline() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 297.0, 365);

        placeOrder(student, "STUDENT", courseBody(course, "CARD", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("APPROVED")))
                .andExpect(jsonPath("$.data.itemType", is("COURSE")))
                .andExpect(jsonPath("$.data.courseId", is(course.toString())))
                .andExpect(jsonPath("$.data.seatsQuantity", nullValue()))
                .andExpect(jsonPath("$.data.amount", is(297.0)))
                .andExpect(jsonPath("$.data.paymentMethod", is("CARD")))
                .andExpect(jsonPath("$.data.installments", is(3)))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()));

        final Map<String, Object> order = jdbcTemplate.queryForMap(
                "select type, status, buyer_user_id, company_id, amount from orders");
        assertEquals("INDIVIDUAL", order.get("type"));
        assertEquals("APPROVED", order.get("status"));
        assertEquals(student, order.get("buyer_user_id"));
        assertNull(order.get("company_id"));
        assertEquals(1, count("order_status_history"));

        final Map<String, Object> enrollment = jdbcTemplate.queryForMap(
                "select progress, dt_inicio, dt_expiracao from user_courses where user_id = ? and course_id = ?",
                student, course);
        assertEquals(0, enrollment.get("progress"));
        assertEquals(LocalDate.now(), ((Date) enrollment.get("dt_inicio")).toLocalDate());
        assertEquals(LocalDate.now().plusDays(365), ((Date) enrollment.get("dt_expiracao")).toLocalDate());
        assertEquals(0, count("company_courses"));
    }

    @Test
    void shouldShowThePurchasedCourseInMyCourses() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 297.0, 365);

        placeOrder(student, "STUDENT", courseBody(course, "CARD", 1)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/student/enrollments").header("Authorization", bearer(student, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].courseId", is(course.toString())));
    }

    @Test
    void shouldEnrollWithoutAnEndDateWhenTheCourseHasNoDeadline() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 100.0, null);

        placeOrder(student, "STUDENT", courseBody(course, "CARD", 1)).andExpect(status().isCreated());

        assertNull(jdbcTemplate.queryForObject(
                "select dt_expiracao from user_courses where user_id = ?", Date.class, student));
    }

    @Test
    void shouldIgnoreAnAmountSentByTheClientAndChargeTheCoursePrice() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 297.0, 30);
        final String body = """
                {"itemType": "COURSE", "courseId": "%s", "paymentMethod": "CARD", "installments": 1, "amount": 1.00}
                """.formatted(course);

        placeOrder(student, "STUDENT", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount", is(297.0)));

        assertEquals(0, new java.math.BigDecimal("297.00").compareTo(
                jdbcTemplate.queryForObject("select amount from orders", java.math.BigDecimal.class)));
    }

    @Test
    void shouldRegisterTheCourseOfACompanyWithoutAnEndDate() throws Exception {
        final UUID company = givenCompany();
        final UUID course = givenCourse("PUBLISHED", 297.0, 365);

        placeOrder(company, "COMPANY", courseBody(course, "CARD", 1)).andExpect(status().isCreated());

        final Map<String, Object> order = jdbcTemplate.queryForMap("select type, company_id from orders");
        assertEquals("CORPORATE", order.get("type"));
        assertEquals(company, order.get("company_id"));
        final Map<String, Object> companyCourse = jdbcTemplate.queryForMap(
                "select data_inicio, data_expiracao from company_courses where company_id = ?", company);
        assertEquals(LocalDate.now(), ((Date) companyCourse.get("data_inicio")).toLocalDate());
        assertNull(companyCourse.get("data_expiracao"));
        assertEquals(0, count("user_courses"));
    }

    @Test
    void shouldAddTheSeatsOfACompanyWithoutReleasingAnyCourse() throws Exception {
        final UUID company = givenCompany();

        placeOrder(company, "COMPANY", seatsBody(10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.itemType", is("SEATS")))
                .andExpect(jsonPath("$.data.seatsQuantity", is(10)))
                .andExpect(jsonPath("$.data.courseId", nullValue()))
                .andExpect(jsonPath("$.data.amount", is(790.0)));

        placeOrder(company, "COMPANY", seatsBody(5)).andExpect(status().isCreated());

        assertEquals(15, jdbcTemplate.queryForObject(
                "select seats_total from company where id = ?", Integer.class, company));
        assertEquals(0, count("company_courses"));
        assertEquals(0, count("user_courses"));
    }

    @Test
    void shouldRefuseSeatsBoughtByAStudent() throws Exception {
        final UUID student = givenUser("STUDENT");

        placeOrder(student, "STUDENT", seatsBody(10)).andExpect(status().isBadRequest());

        assertEquals(0, count("orders"));
    }

    @Test
    void shouldRefuseAPaymentMethodOtherThanCard() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 100.0, 30);

        placeOrder(student, "STUDENT", courseBody(course, "PIX", 1)).andExpect(status().isBadRequest());
        placeOrder(student, "STUDENT", courseBody(course, "BOLETO", 1)).andExpect(status().isBadRequest());
        placeOrder(student, "STUDENT", courseBody(course, "CASH", 1)).andExpect(status().isBadRequest());

        assertEquals(0, count("orders"));
        assertEquals(0, count("user_courses"));
    }

    @Test
    void shouldNameTheFieldWhenTheEnumValueDoesNotExist() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 100.0, 30);

        placeOrder(student, "STUDENT", courseBody(course, "CASH", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid value for field 'paymentMethod'")));
    }

    @Test
    void shouldReturnBadRequestForAMalformedBody() throws Exception {
        final UUID student = givenUser("STUDENT");

        placeOrder(student, "STUDENT", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Malformed request body")));
    }

    @Test
    void shouldRefuseABodyWithoutTheRequiredFields() throws Exception {
        final UUID student = givenUser("STUDENT");

        placeOrder(student, "STUDENT", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("required")));
    }

    @Test
    void shouldReturnNotFoundForACourseThatDoesNotExist() throws Exception {
        final UUID student = givenUser("STUDENT");

        placeOrder(student, "STUDENT", courseBody(UUID.randomUUID(), "CARD", 1)).andExpect(status().isNotFound());

        assertEquals(0, count("orders"));
    }

    @Test
    void shouldRefuseACourseThatIsNotPublished() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID draft = givenCourse("DRAFT", 100.0, 30);
        final UUID archived = givenCourse("ARCHIVED", 100.0, 30);

        placeOrder(student, "STUDENT", courseBody(draft, "CARD", 1)).andExpect(status().isBadRequest());
        placeOrder(student, "STUDENT", courseBody(archived, "CARD", 1)).andExpect(status().isBadRequest());

        assertEquals(0, count("orders"));
    }

    @Test
    void shouldReturnConflictWhenTheStudentBuysACourseThatIsStillActive() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 100.0, 30);
        placeOrder(student, "STUDENT", courseBody(course, "CARD", 1)).andExpect(status().isCreated());

        placeOrder(student, "STUDENT", courseBody(course, "CARD", 1)).andExpect(status().isConflict());

        assertEquals(1, count("orders"));
        assertEquals(1, count("user_courses"));
    }

    @Test
    void shouldReturnConflictWhenTheCompanyBuysACourseItAlreadyHas() throws Exception {
        final UUID company = givenCompany();
        final UUID course = givenCourse("PUBLISHED", 100.0, 30);
        placeOrder(company, "COMPANY", courseBody(course, "CARD", 1)).andExpect(status().isCreated());

        placeOrder(company, "COMPANY", courseBody(course, "CARD", 1)).andExpect(status().isConflict());

        assertEquals(1, count("orders"));
    }

    @Test
    void shouldRenewAnExpiredEnrollmentKeepingTheProgress() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 100.0, 30);
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, dt_inicio, dt_expiracao, progress)"
                        + " values (?, ?, ?, ?, 40)",
                student, course, Date.valueOf(LocalDate.now().minusDays(90)), Date.valueOf(LocalDate.now().minusDays(1)));

        placeOrder(student, "STUDENT", courseBody(course, "CARD", 1)).andExpect(status().isCreated());

        final Map<String, Object> enrollment = jdbcTemplate.queryForMap(
                "select progress, dt_inicio, dt_expiracao from user_courses where user_id = ?", student);
        assertEquals(40, enrollment.get("progress"));
        assertEquals(LocalDate.now(), ((Date) enrollment.get("dt_inicio")).toLocalDate());
        assertEquals(LocalDate.now().plusDays(30), ((Date) enrollment.get("dt_expiracao")).toLocalDate());
        assertEquals(1, count("user_courses"));
    }

    @Test
    void shouldReturnUnauthorizedWithoutAToken() throws Exception {
        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(seatsBody(1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenForAnAdmin() throws Exception {
        final UUID admin = givenUser("ADMIN");
        final UUID course = givenCourse("PUBLISHED", 100.0, 30);

        placeOrder(admin, "ADMIN", courseBody(course, "CARD", 1)).andExpect(status().isForbidden());

        assertEquals(0, count("orders"));
    }

    @Test
    void shouldKeepTheOrderHistoryWithTheApprovalEntry() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse("PUBLISHED", 100.0, 30);

        placeOrder(student, "STUDENT", courseBody(course, "CARD", 1)).andExpect(status().isCreated());

        final List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select status, changed_by from order_status_history");
        assertEquals(1, history.size());
        assertEquals("APPROVED", history.get(0).get("status"));
        assertNull(history.get(0).get("changed_by"));
        mockMvc.perform(get("/api/v1/student/enrollments").header("Authorization", bearer(student, "STUDENT")))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    void shouldGiveACompanyTheConfiguredSeatPriceUsedByTheSeatsOrder() throws Exception {
        final UUID company = givenCompany();

        mockMvc.perform(get(ORDERS + "/seat-price").header("Authorization", bearer(company, "COMPANY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seatPrice", is(79.0)));
        placeOrder(company, "COMPANY", seatsBody(2))
                .andExpect(jsonPath("$.data.amount", is(158.0)));
    }

    @Test
    void shouldRequireAuthenticationToReadTheSeatPrice() throws Exception {
        mockMvc.perform(get(ORDERS + "/seat-price")).andExpect(status().isUnauthorized());
    }
}
