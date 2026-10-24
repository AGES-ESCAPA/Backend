package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.port.OrderAccessPort;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uma falha em qualquer etapa desfaz o pedido e a liberação de acesso. A liberação é forçada a falhar
 * depois que o pedido já foi gravado, e o teste confere que nada ficou no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderRollbackTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private OrderAccessPort orderAccessPort;

    // O banco é compartilhado entre as classes de teste e pedidos não têm ON DELETE CASCADE.
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from company_courses");
        jdbcTemplate.update("delete from user_courses");
    }

    private UUID givenUser(String role) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role) values (?, 'Comprador', ?, 'hash', ?)",
                id, id + "@email.com", role);
        return id;
    }

    private UUID givenCompany() {
        final UUID id = givenUser("COMPANY");
        jdbcTemplate.update("insert into company (id, company_name) values (?, 'Hotel Teste LTDA')", id);
        return id;
    }

    private UUID givenCourse() {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into courses (id, title, status, price, deadline) values (?, 'Curso', 'PUBLISHED', 100, 30)", id);
        return id;
    }

    private void placeOrder(UUID userId, String role, String body, int expectedStatus) throws Exception {
        final String token = tokenProviderPort.generateToken(userId, userId + "@email.com", role, role);
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Test
    void shouldUndoTheOrderWhenReleasingTheStudentEnrollmentFails() throws Exception {
        final UUID student = givenUser("STUDENT");
        final UUID course = givenCourse();
        doThrow(new IllegalStateException("simulated failure"))
                .when(orderAccessPort).saveStudentEnrollment(any(), any(), any(), any());

        placeOrder(student, "STUDENT", """
                {"itemType": "COURSE", "courseId": "%s", "paymentMethod": "CARD", "installments": 1}
                """.formatted(course), 500);

        assertEquals(0, count("orders"));
        assertEquals(0, count("order_status_history"));
        assertEquals(0, count("user_courses"));
    }

    @Test
    void shouldUndoTheOrderWhenReleasingTheCompanyCourseFails() throws Exception {
        final UUID company = givenCompany();
        final UUID course = givenCourse();
        doThrow(new IllegalStateException("simulated failure"))
                .when(orderAccessPort).saveCompanyCourse(any(), any(), any(), any());

        placeOrder(company, "COMPANY", """
                {"itemType": "COURSE", "courseId": "%s", "paymentMethod": "CARD", "installments": 1}
                """.formatted(course), 500);

        assertEquals(0, count("orders"));
        assertEquals(0, count("company_courses"));
    }

    @Test
    void shouldUndoTheOrderWhenAddingTheSeatsFails() throws Exception {
        final UUID company = givenCompany();
        doThrow(new IllegalStateException("simulated failure"))
                .when(orderAccessPort).addCompanySeats(any(), anyInt());

        placeOrder(company, "COMPANY", """
                {"itemType": "SEATS", "seatsQuantity": 10, "paymentMethod": "CARD", "installments": 1}
                """, 500);

        assertEquals(0, count("orders"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select seats_total from company where id = ?", Integer.class, company));
    }
}
