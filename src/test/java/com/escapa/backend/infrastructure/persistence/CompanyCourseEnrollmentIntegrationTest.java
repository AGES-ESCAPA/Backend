package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.escapa.backend.application.model.CreateOrderCommand;
import com.escapa.backend.application.usecase.CreateOrderUseCase;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.PaymentMethod;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

/** Matrícula dos colaboradores na compra de curso pela empresa (#87), contra o banco real. */
class CompanyCourseEnrollmentIntegrationTest extends PostgresIntegrationTest {

    private static final int DEADLINE_DAYS = 30;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;
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

    private UUID givenUser(String role, String status) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into users (id, name, email, password_hash, role, status) values (?, ?, ?, ?, ?, ?)",
                id, "Usuario", "u-" + id + "@email.com", "hash", role, status);
        return id;
    }

    private UUID givenCompany() {
        final UUID companyId = givenUser("COMPANY", "ACTIVE");
        jdbcTemplate.update("insert into company (id, company_name, cnpj_id) values (?, ?, ?)",
                companyId, "Hotel Teste LTDA", UUID.randomUUID().toString().substring(0, 14));
        return companyId;
    }

    private UUID givenEmployee(UUID companyId, String status) {
        final UUID employeeId = givenUser("STUDENT", status);
        jdbcTemplate.update("insert into users_company (user_id, company_id, role) values (?, ?, 'Recepção')",
                employeeId, companyId);
        return employeeId;
    }

    private UUID givenPublishedCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso " + UUID.randomUUID());
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPrice(97.0);
        course.setDeadline(DEADLINE_DAYS);
        return courseJpaRepository.save(course).getId();
    }

    private void buyCourse(UUID companyId, UUID courseId) {
        createOrderUseCase.execute(new CreateOrderCommand(
                companyId, "COMPANY", OrderItemType.COURSE, courseId, null, PaymentMethod.CARD, 1));
    }

    private Map<String, Object> enrollmentRow(UUID userId, UUID courseId) {
        return jdbcTemplate.queryForMap(
                "select dt_inicio, dt_expiracao, progress, revoked_at from user_courses "
                        + "where user_id = ? and course_id = ?", userId, courseId);
    }

    private int enrollmentCount(UUID courseId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from user_courses where course_id = ?", Integer.class, courseId);
    }

    private static LocalDate date(Object value) {
        return ((Date) value).toLocalDate();
    }

    @Test
    void shouldEnrollEveryLinkedEmployeeOfAnyStatusInTheSameOrder() {
        final UUID companyId = givenCompany();
        final UUID active = givenEmployee(companyId, "ACTIVE");
        final UUID pending = givenEmployee(companyId, "PENDING");
        final UUID inactive = givenEmployee(companyId, "INACTIVE");
        final UUID courseId = givenPublishedCourse();

        buyCourse(companyId, courseId);

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from orders where company_id = ? and course_id = ?", Integer.class, companyId, courseId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from company_courses where company_id = ? and course_id = ?",
                Integer.class, companyId, courseId));
        assertEquals(3, enrollmentCount(courseId));
        for (final UUID employeeId : new UUID[] {active, pending, inactive}) {
            final Map<String, Object> row = enrollmentRow(employeeId, courseId);
            assertEquals(LocalDate.now(), date(row.get("dt_inicio")));
            assertEquals(LocalDate.now().plusDays(DEADLINE_DAYS), date(row.get("dt_expiracao")));
            assertEquals(0, row.get("progress"));
            assertNull(row.get("revoked_at"));
        }
    }

    @Test
    void shouldReactivateARevokedEnrollmentKeepingTheProgress() {
        final UUID companyId = givenCompany();
        final UUID employeeId = givenEmployee(companyId, "ACTIVE");
        final UUID courseId = givenPublishedCourse();
        jdbcTemplate.update("insert into user_courses (user_id, course_id, progress, dt_inicio, dt_expiracao, revoked_at) "
                + "values (?, ?, 40, current_date - 90, current_date - 60, now())", employeeId, courseId);

        buyCourse(companyId, courseId);

        final Map<String, Object> row = enrollmentRow(employeeId, courseId);
        assertNull(row.get("revoked_at"));
        assertEquals(40, row.get("progress"));
        assertEquals(LocalDate.now(), date(row.get("dt_inicio")));
        assertEquals(LocalDate.now().plusDays(DEADLINE_DAYS), date(row.get("dt_expiracao")));
        assertEquals(1, enrollmentCount(courseId));
    }

    @Test
    void shouldKeepTheActiveEnrollmentOfAnEmployeeUntouched() {
        final UUID companyId = givenCompany();
        final UUID employeeId = givenEmployee(companyId, "ACTIVE");
        final UUID courseId = givenPublishedCourse();
        jdbcTemplate.update("insert into user_courses (user_id, course_id, progress, dt_inicio, dt_expiracao) "
                + "values (?, ?, 70, current_date - 10, current_date + 200)", employeeId, courseId);

        buyCourse(companyId, courseId);

        final Map<String, Object> row = enrollmentRow(employeeId, courseId);
        assertEquals(70, row.get("progress"));
        assertEquals(LocalDate.now().minusDays(10), date(row.get("dt_inicio")));
        assertEquals(LocalDate.now().plusDays(200), date(row.get("dt_expiracao")));
        assertEquals(1, enrollmentCount(courseId));
    }
}
