package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CourseRepositoryPort courseRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @BeforeEach
    void cleanUp() {
        courseJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void shouldReturnOnlyPublishedCourses() {
        final AdminEntity instructor = createInstructor("Instrutora Publicada");
        createCourse("Curso Publicado", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso Rascunho", "Turismo", "INICIANTE", CourseStatus.DRAFT, instructor);
        createCourse("Curso Arquivado", "Turismo", "INICIANTE", CourseStatus.ARCHIVED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Curso Publicado", result.content().get(0).title());
    }

    @Test
    void shouldFilterByTitleCaseInsensitive() {
        final AdminEntity instructor = createInstructor("Instrutora Titulo");
        createCourse("Atendimento de Excelência", "Hospitalidade", "INICIANTE",
                CourseStatus.PUBLISHED, instructor);
        createCourse("Gestão de Reservas", "Turismo", "INTERMEDIÁRIO",
                CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                "atendimento", null, null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Atendimento de Excelência", result.content().get(0).title());
    }

    @Test
    void shouldFilterByCategory() {
        final AdminEntity instructor = createInstructor("Instrutora Categoria");
        createCourse("Curso A", "Hospitalidade", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso B", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, "Hospitalidade", null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Curso A", result.content().get(0).title());
    }

    @Test
    void shouldFilterByLevel() {
        final AdminEntity instructor = createInstructor("Instrutora Level");
        createCourse("Curso Ini", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso Adv", "Turismo", "AVANÇADO", CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, "INICIANTE", 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Curso Ini", result.content().get(0).title());
    }

    @Test
    void shouldPaginateResults() {
        final AdminEntity instructor = createInstructor("Instrutora Paginacao");
        createCourse("Curso 1", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso 2", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso 3", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> firstPage = courseRepositoryPort.findPublished(
                null, null, null, 0, 2);

        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.content().size());
        assertEquals(2, firstPage.totalPages());

        final PageResult<CourseSummary> secondPage = courseRepositoryPort.findPublished(
                null, null, null, 1, 2);

        assertEquals(1, secondPage.content().size());
    }

    @Test
    void shouldIncludeInstructorName() {
        final AdminEntity instructor = createInstructor("Dra. Mariana");
        createCourse("Curso com Instrutor", "Turismo", "INICIANTE",
                CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        assertEquals("Dra. Mariana", result.content().get(0).instructorName());
    }

    @Test
    void shouldReturnEmptyWhenNoPublishedCourses() {
        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void shouldReturnDenormalizedCounters() {
        final AdminEntity instructor = createInstructor("Instrutora Contadores");
        final CourseEntity course = buildCourse("Curso Contadores", "Turismo", "INICIANTE",
                CourseStatus.PUBLISHED, instructor);
        course.setLessonsCount(5);
        course.setReviewsCount(10);
        course.setRatingAverage(4.5);
        courseJpaRepository.save(course);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        final CourseSummary summary = result.content().get(0);
        assertEquals(5, summary.lessonsCount());
        assertEquals(10, summary.reviewsCount());
        assertEquals(4.5, summary.ratingAverage());
    }

    private AdminEntity createInstructor(String name) {
        final String uniqueEmail = name.toLowerCase().replace(" ", ".").replace(".", "")
                + UUID.randomUUID().toString().substring(0, 4) + "@escapa.com";
        final AdminEntity admin = new AdminEntity(
                UUID.randomUUID(), name, uniqueEmail, "hash", "ADMIN",
                LocalDateTime.now(), "Departamento");
        return userJpaRepository.save(admin);
    }

    private void createCourse(String title, String category, String level,
                              CourseStatus status, AdminEntity instructor) {
        final CourseEntity course = buildCourse(title, category, level, status, instructor);
        courseJpaRepository.save(course);
    }

    private CourseEntity buildCourse(String title, String category, String level,
                                    CourseStatus status, AdminEntity instructor) {
        final CourseEntity course = new CourseEntity();
        course.setId(UUID.randomUUID());
        course.setTitle(title);
        course.setShortDescription("Descrição de " + title);
        course.setCategory(category);
        course.setLevel(level);
        course.setStatus(status);
        course.setInstructor(instructor);
        course.setCreatedBy(instructor);
        course.setCreatedAt(LocalDateTime.now());
        course.setPrice(99.90);
        return course;
    }
}
