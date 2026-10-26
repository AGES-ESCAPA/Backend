package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CompanySeatsPort;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.OrderAccessRevocationPort;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.CancelOrderUseCase;
import com.escapa.backend.application.usecase.RefundOrderUseCase;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;
import com.escapa.backend.domain.order.SeatsInUseException;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cancelamento e reembolso de pedidos contra o banco real (US-28). */
class OrderCancellationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderAccessRevocationPort revocationPort;
    @Autowired
    private CompanySeatsPort seatsPort;
    @Autowired
    private EnrollmentRepositoryPort enrollmentRepositoryPort;
    @Autowired
    private OrderRepositoryPort orderRepositoryPort;
    @Autowired
    private UserRepositoryPort userRepositoryPort;
    @Autowired
    private CourseJpaRepository courseJpaRepository;
    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;
    @Autowired
    private RefundOrderUseCase refundOrderUseCase;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Pedidos não têm ON DELETE CASCADE: sem esta limpeza, outros testes que fazem
    // deleteAll em users/courses quebram (o banco é compartilhado entre as classes).
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from courses");
        jdbcTemplate.update("delete from users");
    }

    private UUID givenUser(String role) {
        return userRepositoryPort.save(
                new User("Usuario", "u-" + UUID.randomUUID() + "@email.com", "hash", role)).getId();
    }

    private UUID givenCompany(int seatsTotal) {
        final UUID companyId = givenUser("COMPANY");
        jdbcTemplate.update(
                "insert into company (id, company_name, cnpj_id, seats_total) values (?, ?, ?, ?)",
                companyId, "Hotel Teste LTDA", UUID.randomUUID().toString().substring(0, 14), seatsTotal);
        return companyId;
    }

    private UUID givenCollaborator(UUID companyId) {
        final UUID userId = givenUser("STUDENT");
        jdbcTemplate.update("insert into users_company (user_id, company_id, role) values (?, ?, 'RECEPCAO')",
                userId, companyId);
        return userId;
    }

    private UUID givenCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso " + UUID.randomUUID());
        return courseJpaRepository.save(course).getId();
    }

    private void givenEnrollment(UUID userId, UUID courseId, int progress) {
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, dt_inicio, progress, certificate_issued) "
                        + "values (?, ?, current_date, ?, false)", userId, courseId, progress);
    }

    private void givenCompanyCourse(UUID companyId, UUID courseId) {
        jdbcTemplate.update("insert into company_courses (company_id, course_id, data_inicio) "
                + "values (?, ?, current_date)", companyId, courseId);
    }

    private Order givenApprovedOrder(OrderType type, OrderItemType itemType,
                                     UUID buyerId, UUID companyId, UUID courseId, Integer seats) {
        final Order order = new Order();
        order.setBuyerUserId(buyerId);
        order.setCompanyId(companyId);
        order.setType(type);
        order.setItemType(itemType);
        order.setCourseId(courseId);
        order.setSeatsQuantity(seats);
        order.setAmount(new BigDecimal("97.00"));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setInstallments(1);
        order.changeStatus(OrderStatus.APPROVED, null, null);
        return orderRepositoryPort.save(order);
    }

    private LocalDateTime revokedAt(String table, String ownerColumn, UUID ownerId, UUID courseId) {
        return jdbcTemplate.queryForObject(
                "select revoked_at from " + table + " where " + ownerColumn + " = ? and course_id = ?",
                LocalDateTime.class, ownerId, courseId);
    }

    @Test
    void shouldRevokeAStudentEnrollmentKeepingTheProgressAndHidingItFromTheReaders() {
        final UUID studentId = givenUser("STUDENT");
        final UUID revoked = givenCourse();
        final UUID untouched = givenCourse();
        givenEnrollment(studentId, revoked, 40);
        givenEnrollment(studentId, untouched, 10);

        revocationPort.revokeStudentCourseAccess(studentId, revoked, LocalDateTime.now());

        assertNotNull(revokedAt("user_courses", "user_id", studentId, revoked));
        assertEquals(40, jdbcTemplate.queryForObject(
                "select progress from user_courses where user_id = ? and course_id = ?",
                Integer.class, studentId, revoked));
        assertTrue(enrollmentRepositoryPort.findByUserIdAndCourseId(studentId, revoked).isEmpty());
        assertTrue(enrollmentRepositoryPort.findByUserIdAndCourseId(studentId, untouched).isPresent());
        assertEquals(1, enrollmentRepositoryPort.findStudentEnrollments(studentId, null, null, 0, 10).totalElements());
    }

    @Test
    void shouldRevokeTheCompanyCourseAndOnlyTheCollaboratorsEnrollments() {
        final UUID companyId = givenCompany(0);
        final UUID collaboratorId = givenCollaborator(companyId);
        final UUID outsiderId = givenUser("STUDENT");
        final UUID courseId = givenCourse();
        givenCompanyCourse(companyId, courseId);
        givenEnrollment(collaboratorId, courseId, 20);
        givenEnrollment(outsiderId, courseId, 30);

        revocationPort.revokeCompanyCourseAccess(companyId, courseId, LocalDateTime.now());

        assertNotNull(revokedAt("company_courses", "company_id", companyId, courseId));
        assertNotNull(revokedAt("user_courses", "user_id", collaboratorId, courseId));
        assertNull(revokedAt("user_courses", "user_id", outsiderId, courseId));
    }

    @Test
    void shouldCountTheCollaboratorsAsSeatsInUse() {
        final UUID companyId = givenCompany(10);
        givenCollaborator(companyId);
        givenCollaborator(companyId);

        assertEquals(10, seatsPort.findSeatsTotal(companyId));
        assertEquals(2, seatsPort.countSeatsInUse(companyId));

        seatsPort.updateSeatsTotal(companyId, 4);

        assertEquals(4, seatsPort.findSeatsTotal(companyId));
    }

    @Test
    void shouldReadTheOrderForUpdateWithItsHistory() {
        final Order order = givenApprovedOrder(
                OrderType.INDIVIDUAL, OrderItemType.COURSE, givenUser("STUDENT"), null, givenCourse(), null);

        final Order found = orderRepositoryPort.findByIdForUpdate(order.getId()).orElseThrow();

        assertEquals(OrderStatus.APPROVED, found.getStatus());
        assertEquals(1, found.getStatusHistory().size());
        assertTrue(orderRepositoryPort.findByIdForUpdate(UUID.randomUUID()).isEmpty());
    }

    @Test
    void shouldCancelAndRefundAnIndividualOrderEndToEnd() {
        final UUID studentId = givenUser("STUDENT");
        final UUID adminId = givenUser("ADMIN");
        final UUID courseId = givenCourse();
        givenEnrollment(studentId, courseId, 55);
        final Order order = givenApprovedOrder(OrderType.INDIVIDUAL, OrderItemType.COURSE, studentId, null, courseId, null);

        cancelOrderUseCase.execute(order.getId(), adminId, "Pedido do cliente");

        final Order cancelled = orderRepositoryPort.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(2, cancelled.getStatusHistory().size());
        assertEquals(adminId, cancelled.getStatusHistory().get(1).changedBy());
        assertNotNull(revokedAt("user_courses", "user_id", studentId, courseId));

        refundOrderUseCase.execute(order.getId(), adminId, null);

        assertEquals(OrderStatus.REFUNDED, orderRepositoryPort.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(3, orderRepositoryPort.findById(order.getId()).orElseThrow().getStatusHistory().size());
    }

    @Test
    void shouldCancelACorporateCourseOrderRevokingTheCompanyAndItsCollaborators() {
        final UUID companyId = givenCompany(0);
        final UUID collaboratorId = givenCollaborator(companyId);
        final UUID courseId = givenCourse();
        givenCompanyCourse(companyId, courseId);
        givenEnrollment(collaboratorId, courseId, 25);
        final Order order = givenApprovedOrder(OrderType.CORPORATE, OrderItemType.COURSE, collaboratorId, companyId, courseId, null);

        cancelOrderUseCase.execute(order.getId(), givenUser("ADMIN"), null);

        assertNotNull(revokedAt("company_courses", "company_id", companyId, courseId));
        assertNotNull(revokedAt("user_courses", "user_id", collaboratorId, courseId));
    }

    @Test
    void shouldRefuseToCancelSeatsInUseLeavingEverythingUntouched() {
        final UUID companyId = givenCompany(10);
        givenCollaborator(companyId);
        final Order order = givenApprovedOrder(OrderType.CORPORATE, OrderItemType.SEATS, givenUser("STUDENT"), companyId, null, 10);
        final UUID adminId = givenUser("ADMIN");

        final SeatsInUseException error = assertThrows(SeatsInUseException.class,
                () -> cancelOrderUseCase.execute(order.getId(), adminId, null));

        assertEquals(1, error.getSeatsToRelease());
        assertEquals(10, seatsPort.findSeatsTotal(companyId));
        assertEquals(OrderStatus.APPROVED, orderRepositoryPort.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldCancelSeatsSubtractingThemFromTheCompanyTotal() {
        final UUID companyId = givenCompany(10);
        givenCollaborator(companyId);
        final Order order = givenApprovedOrder(OrderType.CORPORATE, OrderItemType.SEATS, givenUser("STUDENT"), companyId, null, 5);

        cancelOrderUseCase.execute(order.getId(), givenUser("ADMIN"), null);

        assertEquals(5, seatsPort.findSeatsTotal(companyId));
        assertEquals(OrderStatus.CANCELLED, orderRepositoryPort.findById(order.getId()).orElseThrow().getStatus());
    }
}
