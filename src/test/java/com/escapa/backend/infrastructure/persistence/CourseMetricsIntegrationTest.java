package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CourseEnrollmentCount;
import com.escapa.backend.application.model.CourseMetrics;
import com.escapa.backend.application.model.CourseMetrics.CategoryShare;
import com.escapa.backend.application.model.CourseMetrics.CourseRanking;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.GetAdminCourseMetricsUseCase;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseMetricsIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private EnrollmentRepositoryPort enrollmentRepositoryPort;

    @Autowired
    private GetAdminCourseMetricsUseCase getAdminCourseMetricsUseCase;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserCourseJpaRepository userCourseJpaRepository;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from user_courses");
        jdbcTemplate.update("delete from company_courses");
        jdbcTemplate.update("delete from courses");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldCountOnlyNonRevokedEnrollmentsOfNonDraftCoursesFromUserCourses() {
        final CourseEntity publishedCourse = givenCourse("Turismo de Aventura", "Hospitalidade", CourseStatus.PUBLISHED);
        final CourseEntity archivedCourse = givenCourse("IA no Turismo", "Inteligência Artificial", CourseStatus.ARCHIVED);
        final CourseEntity draftCourse = givenCourse("Curso Rascunho", "Hospitalidade", CourseStatus.DRAFT);
        final CourseEntity emptyCourse = givenCourse("Curso Sem Alunos", "Gestão", CourseStatus.PUBLISHED);

        final UUID student1 = givenUser("STUDENT");
        final UUID student2 = givenUser("STUDENT");
        final UUID collaborator = givenCollaborator();
        final UUID revokedStudent = givenUser("STUDENT");

        givenEnrollment(student1, publishedCourse, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 10), null);
        givenEnrollment(collaborator, publishedCourse, LocalDate.now(), null, null);
        givenEnrollment(revokedStudent, publishedCourse, LocalDate.now(), null, LocalDateTime.now());
        givenEnrollment(student2, archivedCourse, LocalDate.now(), null, null);
        givenEnrollment(student1, draftCourse, LocalDate.now(), null, null);

        final List<CourseEnrollmentCount> counts = enrollmentRepositoryPort.countEligibleEnrollmentsByCourse();
        final Map<UUID, Long> enrollmentsByCourse = counts.stream()
                .collect(Collectors.toMap(CourseEnrollmentCount::courseId, CourseEnrollmentCount::enrollments));

        assertEquals(2, counts.size());
        assertEquals(2L, enrollmentsByCourse.get(publishedCourse.getId()));
        assertEquals(1L, enrollmentsByCourse.get(archivedCourse.getId()));
        assertNull(enrollmentsByCourse.get(draftCourse.getId()));
        assertNull(enrollmentsByCourse.get(emptyCourse.getId()));

        final CourseMetrics metrics = getAdminCourseMetricsUseCase.execute();
        assertEquals(
                new CourseRanking(publishedCourse.getId(), "Turismo de Aventura", 2L),
                metrics.mostAccessedCourse());
        assertEquals(2, metrics.categoriesCount());
        assertEquals(List.of(
                new CategoryShare("Hospitalidade", new BigDecimal("66.67")),
                new CategoryShare("Inteligência Artificial", new BigDecimal("33.33"))
        ), metrics.enrollmentsByCategory());
    }

    @Test
    void shouldReturnEmptyMetricsWhenNoEligibleEnrollmentsExistInDatabase() {
        final CourseEntity draftCourse = givenCourse("Rascunho", "Turismo", CourseStatus.DRAFT);
        final CourseEntity publishedCourse = givenCourse("Publicado", "Turismo", CourseStatus.PUBLISHED);
        final UUID student = givenUser("STUDENT");

        givenEnrollment(student, draftCourse, LocalDate.now(), null, null);
        givenEnrollment(student, publishedCourse, LocalDate.now(), null, LocalDateTime.now());

        final CourseMetrics metrics = getAdminCourseMetricsUseCase.execute();

        assertNull(metrics.mostAccessedCourse());
        assertTrue(metrics.topCourses().isEmpty());
        assertEquals(0, metrics.categoriesCount());
        assertTrue(metrics.enrollmentsByCategory().isEmpty());
    }

    private CourseEntity givenCourse(String title, String category, CourseStatus status) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setCategory(category);
        course.setStatus(status);
        return courseJpaRepository.save(course);
    }

    private UUID givenUser(String role) {
        final String email = "u-" + UUID.randomUUID() + "@escapa.com";
        return userRepositoryPort.save(new User("Usuario", email, "hash", role)).getId();
    }

    private UUID givenCollaborator() {
        final UUID companyId = givenUser("COMPANY");
        jdbcTemplate.update(
                "insert into company (id, company_name, cnpj_id, seats_total) values (?, ?, ?, 5)",
                companyId, "Empresa Teste", UUID.randomUUID().toString().substring(0, 14));
        final UUID collaboratorId = givenUser("STUDENT");
        jdbcTemplate.update(
                "insert into users_company (user_id, company_id, role) values (?, ?, 'COLABORADOR')",
                collaboratorId, companyId);
        return collaboratorId;
    }

    private void givenEnrollment(
            UUID userId,
            CourseEntity course,
            LocalDate startDate,
            LocalDate expirationDate,
            LocalDateTime revokedAt
    ) {
        final UserCourseEntity enrollment = new UserCourseEntity();
        enrollment.setId(new UserCourseId(userId, course.getId()));
        enrollment.setUser(userJpaRepository.getReferenceById(userId));
        enrollment.setCourse(course);
        enrollment.setDtInicio(startDate);
        enrollment.setDtExpiracao(expirationDate);
        enrollment.setRevokedAt(revokedAt);
        userCourseJpaRepository.save(enrollment);
    }
}

