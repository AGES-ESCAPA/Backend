package com.escapa.backend.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garante, direto no banco, as restrições da migration V10 (orders e order_status_history). */
class OrdersSchemaTest extends PostgresIntegrationTest {

    private static final String INSERT_ORDER = """
            insert into orders (id, buyer_user_id, company_id, type, item_type, course_id, seats_quantity,
                                amount, payment_method, installments, status)
            values (?, ?, ?, ?, ?, ?, ?, 100.00, ?, 1, ?)
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // O banco é compartilhado entre as classes de teste e pedidos não têm ON DELETE CASCADE:
    // sem esta limpeza, outros testes que fazem deleteAll em users/courses quebram.
    @AfterEach
    void cleanUpOrders() {
        jdbcTemplate.update("delete from orders");
    }

    private UUID givenUser() {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role) values (?, 'Teste', ?, 'hash', 'STUDENT')",
                id, id + "@email.com");
        return id;
    }

    private UUID givenCompany() {
        final UUID id = givenUser();
        jdbcTemplate.update("insert into company (id, company_name) values (?, 'Hotel Teste')", id);
        return id;
    }

    private UUID givenCourse() {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into courses (id, title) values (?, 'Curso Teste')", id);
        return id;
    }

    private void insertOrder(UUID companyId, String type, String itemType, UUID courseId,
                             Integer seats, String paymentMethod, String status) {
        jdbcTemplate.update(INSERT_ORDER, UUID.randomUUID(), givenUser(), companyId, type, itemType,
                courseId, seats, paymentMethod, status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CARD", "PIX", "BOLETO"})
    void shouldAcceptEveryPlannedPaymentMethod(String paymentMethod) {
        assertDoesNotThrow(() -> insertOrder(
                null, "INDIVIDUAL", "COURSE", givenCourse(), null, paymentMethod, "APPROVED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASH", "card", "DEBIT", ""})
    void shouldRejectAnyOtherPaymentMethod(String paymentMethod) {
        final UUID courseId = givenCourse();

        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "INDIVIDUAL", "COURSE", courseId, null, paymentMethod, "APPROVED"));
    }

    @Test
    void shouldRejectUnknownStatusTypeAndItemType() {
        final UUID courseId = givenCourse();

        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "INDIVIDUAL", "COURSE", courseId, null, "CARD", "PENDING"));
        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "PARTNER", "COURSE", courseId, null, "CARD", "APPROVED"));
        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "INDIVIDUAL", "PACKAGE", courseId, null, "CARD", "APPROVED"));
    }

    @Test
    void shouldRejectACourseOrderWithoutCourseId() {
        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "INDIVIDUAL", "COURSE", null, null, "CARD", "APPROVED"));
    }

    @Test
    void shouldRejectACourseOrderThatCarriesASeatsQuantity() {
        final UUID courseId = givenCourse();

        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "INDIVIDUAL", "COURSE", courseId, 5, "CARD", "APPROVED"));
    }

    @Test
    void shouldAcceptACorporateSeatsOrderWithQuantityAndCompany() {
        final UUID companyId = givenCompany();

        assertDoesNotThrow(() -> insertOrder(
                companyId, "CORPORATE", "SEATS", null, 10, "CARD", "APPROVED"));
    }

    @Test
    void shouldRejectASeatsOrderWithoutQuantity() {
        final UUID companyId = givenCompany();

        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                companyId, "CORPORATE", "SEATS", null, null, "CARD", "APPROVED"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3})
    void shouldRejectASeatsOrderWithANonPositiveQuantity(int quantity) {
        final UUID companyId = givenCompany();

        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                companyId, "CORPORATE", "SEATS", null, quantity, "CARD", "APPROVED"));
    }

    @Test
    void shouldRejectASeatsOrderWithoutCompany() {
        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "INDIVIDUAL", "SEATS", null, 10, "CARD", "APPROVED"));
        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "CORPORATE", "SEATS", null, 10, "CARD", "APPROVED"));
    }

    @Test
    void shouldRejectASeatsOrderThatCarriesACourse() {
        final UUID companyId = givenCompany();
        final UUID courseId = givenCourse();

        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                companyId, "CORPORATE", "SEATS", courseId, 10, "CARD", "APPROVED"));
    }

    @Test
    void shouldOnlyAllowACompanyOnCorporateOrders() {
        final UUID companyId = givenCompany();
        final UUID courseId = givenCourse();

        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                companyId, "INDIVIDUAL", "COURSE", courseId, null, "CARD", "APPROVED"));
        assertThrows(DataIntegrityViolationException.class, () -> insertOrder(
                null, "CORPORATE", "COURSE", courseId, null, "CARD", "APPROVED"));
        assertDoesNotThrow(() -> insertOrder(
                companyId, "CORPORATE", "COURSE", courseId, null, "CARD", "APPROVED"));
    }

    @Test
    void shouldRejectANegativeAmountAndZeroInstallments() {
        final UUID courseId = givenCourse();
        final UUID buyerId = givenUser();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into orders (id, buyer_user_id, type, item_type, course_id, amount, payment_method,"
                        + " installments, status) values (?, ?, 'INDIVIDUAL', 'COURSE', ?, -1, 'CARD', 1, 'APPROVED')",
                UUID.randomUUID(), buyerId, courseId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into orders (id, buyer_user_id, type, item_type, course_id, amount, payment_method,"
                        + " installments, status) values (?, ?, 'INDIVIDUAL', 'COURSE', ?, 10, 'CARD', 0, 'APPROVED')",
                UUID.randomUUID(), buyerId, courseId));
    }

    @Test
    void shouldNotStoreAnyCardData() {
        final List<String> columns = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns"
                        + " where table_name in ('orders', 'order_status_history')",
                String.class);

        assertTrue(columns.contains("payment_method"));
        assertTrue(columns.contains("installments"));
        assertTrue(columns.stream().noneMatch(name ->
                name.contains("card") || name.contains("cvv") || name.contains("expir")
                        || name.contains("holder") || name.contains("number") || name.contains("cvc")));
    }

    @Test
    void shouldDeleteTheHistoryWhenTheOrderIsDeleted() {
        final UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(INSERT_ORDER, orderId, givenUser(), null, "INDIVIDUAL", "COURSE",
                givenCourse(), null, "CARD", "APPROVED");
        jdbcTemplate.update(
                "insert into order_status_history (id, order_id, status) values (?, ?, 'APPROVED')",
                UUID.randomUUID(), orderId);

        jdbcTemplate.update("delete from orders where id = ?", orderId);

        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from order_status_history where order_id = ?", Integer.class, orderId));
    }

    @Test
    void shouldKeepTheHistoryEntryWhenTheAuthorIsDeleted() {
        final UUID orderId = UUID.randomUUID();
        final UUID authorId = givenUser();
        final UUID historyId = UUID.randomUUID();
        jdbcTemplate.update(INSERT_ORDER, orderId, givenUser(), null, "INDIVIDUAL", "COURSE",
                givenCourse(), null, "CARD", "APPROVED");
        jdbcTemplate.update(
                "insert into order_status_history (id, order_id, status, changed_by) values (?, ?, 'CANCELLED', ?)",
                historyId, orderId, authorId);

        jdbcTemplate.update("delete from users where id = ?", authorId);

        assertNull(jdbcTemplate.queryForObject(
                "select changed_by from order_status_history where id = ?", UUID.class, historyId));
    }

    @Test
    void shouldRefuseToDeleteAUserOrCourseThatHasOrders() {
        final UUID courseId = givenCourse();
        final UUID buyerId = givenUser();
        jdbcTemplate.update(INSERT_ORDER, UUID.randomUUID(), buyerId, null, "INDIVIDUAL", "COURSE",
                courseId, null, "CARD", "APPROVED");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("delete from users where id = ?", buyerId));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("delete from courses where id = ?", courseId));
    }

    @Test
    void shouldStartCompaniesWithZeroSeatsAndRejectNegativeSeats() {
        final UUID companyId = givenCompany();

        assertEquals(0, jdbcTemplate.queryForObject(
                "select seats_total from company where id = ?", Integer.class, companyId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "update company set seats_total = -1 where id = ?", companyId));
    }
}
