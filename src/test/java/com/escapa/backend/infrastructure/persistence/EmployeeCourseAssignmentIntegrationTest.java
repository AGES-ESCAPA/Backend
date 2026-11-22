package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.escapa.backend.application.model.EmployeeCourseAssignment;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeUnlinkPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

/** Listagem e revogação pontual de curso do colaborador contra o Postgres. */
class EmployeeCourseAssignmentIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private CompanyCourseRepositoryPort companyCourseRepositoryPort;
    @Autowired
    private CompanyEmployeeUnlinkPort companyEmployeeUnlinkPort;
    @Autowired
    private CourseJpaRepository courseJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from courses");
        jdbcTemplate.update("delete from users");
    }

    private UUID givenCompany() {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role, status) values (?, ?, ?, 'hash', 'COMPANY', 'ACTIVE')",
                id, "Empresa", "e-" + id + "@email.com");
        jdbcTemplate.update("insert into company (id, company_name, cnpj_id) values (?, ?, ?)",
                id, "Hotel Teste", UUID.randomUUID().toString().substring(0, 14));
        return id;
    }

    private UUID givenEmployee(UUID companyId) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role, status) values (?, ?, ?, 'hash', 'STUDENT', 'ACTIVE')",
                id, "Colab", "c-" + id + "@email.com");
        jdbcTemplate.update("insert into users_company (user_id, company_id, role) values (?, ?, 'Recepção')",
                id, companyId);
        return id;
    }

    private UUID givenPublishedCourse(String title, String category) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setCategory(category);
        course.setStatus(CourseStatus.PUBLISHED);
        return courseJpaRepository.save(course).getId();
    }

    @Test
    void shouldListPublishedCompanyCoursesWithAssignmentFlag() {
        final UUID companyId = givenCompany();
        final UUID employeeId = givenEmployee(companyId);
        final UUID assigned = givenPublishedCourse("Acesso", "Ops");
        final UUID free = givenPublishedCourse("Livre", "Gestão");
        final UUID archived = givenPublishedCourse("Arquivado", "Ops");
        jdbcTemplate.update("update courses set status = 'ARCHIVED' where id = ?", archived);
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio) values (?, ?, current_date)",
                companyId, assigned);
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio) values (?, ?, current_date)",
                companyId, free);
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio) values (?, ?, current_date)",
                companyId, archived);
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, progress, certificate_issued) values (?, ?, 0, false)",
                employeeId, assigned);

        final List<EmployeeCourseAssignment> result =
                companyCourseRepositoryPort.findEmployeeCourseAssignments(companyId, employeeId);

        assertEquals(2, result.size());
        assertEquals(assigned, result.get(0).courseId());
        assertTrue(result.get(0).assigned());
        assertEquals("Ops", result.get(0).category());
        assertEquals(free, result.get(1).courseId());
        assertFalse(result.get(1).assigned());
    }

    @Test
    void shouldRevokeOnlyTheRequestedCompanyEnrollment() {
        final UUID companyId = givenCompany();
        final UUID employeeId = givenEmployee(companyId);
        final UUID keep = givenPublishedCourse("Manter", "Ops");
        final UUID revoke = givenPublishedCourse("Revogar", "Ops");
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio) values (?, ?, current_date)",
                companyId, keep);
        jdbcTemplate.update(
                "insert into company_courses (company_id, course_id, data_inicio) values (?, ?, current_date)",
                companyId, revoke);
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, progress, certificate_issued) values (?, ?, 10, false)",
                employeeId, keep);
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, progress, certificate_issued) values (?, ?, 20, false)",
                employeeId, revoke);

        assertTrue(companyEmployeeUnlinkPort.revokeCompanyCourseEnrollment(
                companyId, employeeId, revoke, LocalDateTime.of(2026, 11, 21, 12, 0)));
        assertFalse(companyEmployeeUnlinkPort.revokeCompanyCourseEnrollment(
                companyId, employeeId, revoke, LocalDateTime.of(2026, 11, 21, 12, 1)));

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from user_courses where user_id = ? and revoked_at is null",
                Integer.class, employeeId));
        assertEquals(20, jdbcTemplate.queryForObject(
                "select progress from user_courses where user_id = ? and course_id = ?",
                Integer.class, employeeId, revoke));
    }
}
