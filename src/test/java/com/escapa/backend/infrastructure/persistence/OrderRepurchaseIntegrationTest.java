package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CreateOrderCommand;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.CancelOrderUseCase;
import com.escapa.backend.application.usecase.CreateOrderUseCase;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.order.CourseAlreadyActiveException;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.PaymentMethod;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nova compra de um curso cujo pedido foi cancelado (US-27 / US-28), contra o banco real. */
class OrderRepurchaseIntegrationTest extends PostgresIntegrationTest {

    private static final int DEADLINE_DAYS = 30;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;
    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;
    @Autowired
    private EnrollmentRepositoryPort enrollmentRepositoryPort;
    @Autowired
    private UserRepositoryPort userRepositoryPort;
    @Autowired
    private CourseJpaRepository courseJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private UUID givenAdmin() {
        return givenUser("ADMIN");
    }

    private UUID givenCompany() {
        final UUID companyId = givenUser("COMPANY");
        jdbcTemplate.update("insert into company (id, company_name, cnpj_id) values (?, ?, ?)",
                companyId, "Hotel Teste LTDA", UUID.randomUUID().toString().substring(0, 14));
        return companyId;
    }

    private UUID givenPublishedCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso " + UUID.randomUUID());
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPrice(97.0);
        course.setDeadline(DEADLINE_DAYS);
        return courseJpaRepository.save(course).getId();
    }

    private Order buyCourse(UUID buyerId, String role, UUID courseId) {
        return createOrderUseCase.execute(new CreateOrderCommand(
                buyerId, role, OrderItemType.COURSE, courseId, null, PaymentMethod.CARD, 1));
    }

    private Map<String, Object> enrollmentRow(UUID userId, UUID courseId) {
        return jdbcTemplate.queryForMap(
                "select dt_inicio, dt_expiracao, progress, revoked_at from user_courses "
                        + "where user_id = ? and course_id = ?", userId, courseId);
    }

    @Test
    void shouldReactivateTheCancelledEnrollmentWithNewDatesAndTheProgressKept() {
        final UUID studentId = givenUser("STUDENT");
        final UUID courseId = givenPublishedCourse();
        final Order first = buyCourse(studentId, "STUDENT", courseId);
        jdbcTemplate.update("update user_courses set progress = 40, dt_inicio = current_date - 10 "
                + "where user_id = ? and course_id = ?", studentId, courseId);
        cancelOrderUseCase.execute(first.getId(), givenAdmin(), "Pedido cancelado no teste");
        assertTrue(enrollmentRepositoryPort.findByUserIdAndCourseId(studentId, courseId).isEmpty());

        buyCourse(studentId, "STUDENT", courseId);

        final Map<String, Object> row = enrollmentRow(studentId, courseId);
        assertNull(row.get("revoked_at"));
        assertEquals(40, row.get("progress"));
        assertEquals(LocalDate.now(), ((java.sql.Date) row.get("dt_inicio")).toLocalDate());
        assertEquals(LocalDate.now().plusDays(DEADLINE_DAYS), ((java.sql.Date) row.get("dt_expiracao")).toLocalDate());
        assertTrue(enrollmentRepositoryPort.findByUserIdAndCourseId(studentId, courseId).isPresent());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from user_courses where user_id = ? and course_id = ?",
                Integer.class, studentId, courseId));
    }

    @Test
    void shouldStillRefuseBuyingACourseTheStudentHasActive() {
        final UUID studentId = givenUser("STUDENT");
        final UUID courseId = givenPublishedCourse();
        buyCourse(studentId, "STUDENT", courseId);

        assertThrows(CourseAlreadyActiveException.class, () -> buyCourse(studentId, "STUDENT", courseId));
    }

    @Test
    void shouldReactivateTheCancelledCourseOfACompany() {
        final UUID companyId = givenCompany();
        final UUID courseId = givenPublishedCourse();
        final Order first = buyCourse(companyId, "COMPANY", courseId);
        cancelOrderUseCase.execute(first.getId(), givenAdmin(), "Pedido cancelado no teste");

        buyCourse(companyId, "COMPANY", courseId);

        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "select data_inicio, data_expiracao, revoked_at from company_courses "
                        + "where company_id = ? and course_id = ?", companyId, courseId);
        assertNull(row.get("revoked_at"));
        assertNull(row.get("data_expiracao"));
        assertEquals(LocalDate.now(), ((java.sql.Date) row.get("data_inicio")).toLocalDate());
    }
}
