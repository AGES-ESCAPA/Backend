package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CourseMetrics;
import com.escapa.backend.application.model.CourseMetrics.CategoryShare;
import com.escapa.backend.application.model.CourseMetrics.CourseRanking;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.domain.course.CourseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetAdminCourseMetricsUseCaseTest {

    private InMemoryEnrollmentRepositoryPort enrollmentRepository;
    private GetAdminCourseMetricsUseCase useCase;

    @BeforeEach
    void setUp() {
        enrollmentRepository = new InMemoryEnrollmentRepositoryPort();
        useCase = new GetAdminCourseMetricsUseCase(enrollmentRepository);
    }

    @Test
    void shouldReturnEmptyMetricsWhenDatabaseHasNoEnrollments() {
        final CourseMetrics metrics = useCase.execute();

        assertNull(metrics.mostAccessedCourse());
        assertTrue(metrics.topCourses().isEmpty());
        assertEquals(0, metrics.categoriesCount());
        assertTrue(metrics.enrollmentsByCategory().isEmpty());
    }

    @Test
    void shouldRankCoursesByEnrollmentsDescendingAndSetFirstAsMostAccessed() {
        final UUID courseA = UUID.fromString("e0000000-0000-4000-e000-000000000001");
        final UUID courseB = UUID.fromString("e0000000-0000-4000-e000-000000000002");
        enrollmentRepository.registerCourse(courseA, "Turismo de Aventura", "Hospitalidade", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(courseB, "IA no Turismo", "Inteligência Artificial", CourseStatus.ARCHIVED);

        enrollUsers(courseA, 4);
        enrollUsers(courseB, 2);

        final CourseMetrics metrics = useCase.execute();

        assertEquals(new CourseRanking(courseA, "Turismo de Aventura", 4L), metrics.mostAccessedCourse());
        assertEquals(List.of(
                new CourseRanking(courseA, "Turismo de Aventura", 4L),
                new CourseRanking(courseB, "IA no Turismo", 2L)
        ), metrics.topCourses());
        assertEquals(2, metrics.categoriesCount());
        assertEquals(List.of(
                new CategoryShare("Hospitalidade", new BigDecimal("66.67")),
                new CategoryShare("Inteligência Artificial", new BigDecimal("33.33"))
        ), metrics.enrollmentsByCategory());
    }

    @Test
    void shouldLimitTopCoursesToFiveWhileKeepingAllEligibleCoursesInCategoryDistribution() {
        for (int index = 1; index <= 7; index++) {
            final UUID courseId = UUID.fromString(String.format("00000000-0000-4000-8000-00000000000%d", index));
            final String category = index <= 5 ? "Hospitalidade" : "Gestão";
            enrollmentRepository.registerCourse(courseId, "Curso " + index, category, CourseStatus.PUBLISHED);
            enrollUsers(courseId, 8 - index);
        }

        final CourseMetrics metrics = useCase.execute();

        assertEquals(5, metrics.topCourses().size());
        assertEquals(metrics.topCourses().get(0), metrics.mostAccessedCourse());
        assertEquals(7L, metrics.mostAccessedCourse().enrollments());
        assertEquals(3L, metrics.topCourses().get(4).enrollments());
        assertEquals(2, metrics.categoriesCount());
        assertEquals(new BigDecimal("100.00"), sumPercentages(metrics.enrollmentsByCategory()));
    }

    @Test
    void shouldBreakTiesByCourseIdAscendingIncludingHighBitUuids() {
        final UUID lowUuid = UUID.fromString("10000000-0000-4000-8000-000000000001");
        final UUID midUuid = UUID.fromString("70000000-0000-4000-8000-000000000002");
        final UUID highUuid = UUID.fromString("e0000000-0000-4000-e000-000000000003");

        enrollmentRepository.registerCourse(highUuid, "Curso E", "Turismo", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(lowUuid, "Curso 1", "Turismo", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(midUuid, "Curso 7", "Turismo", CourseStatus.PUBLISHED);

        enrollUsers(highUuid, 3);
        enrollUsers(lowUuid, 3);
        enrollUsers(midUuid, 3);

        final CourseMetrics metrics = useCase.execute();

        assertEquals(List.of(lowUuid, midUuid, highUuid),
                metrics.topCourses().stream().map(CourseRanking::courseId).toList());
        assertEquals(lowUuid, metrics.mostAccessedCourse().courseId());
    }

    @Test
    void shouldIgnoreRevokedEnrollmentsAndDraftCoursesWhileIncludingCollaboratorsAndExpired() {
        final UUID publishedCourse = UUID.fromString("e0000000-0000-4000-e000-000000000001");
        final UUID draftCourse = UUID.fromString("e0000000-0000-4000-e000-000000000002");

        enrollmentRepository.registerCourse(publishedCourse, "Curso Publicado", "Gastronomia", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(draftCourse, "Curso Rascunho", "Eventos", CourseStatus.DRAFT);

        final LocalDate past = LocalDate.of(2025, 1, 10);
        enrollmentRepository.enroll(UUID.randomUUID(), publishedCourse, new Enrollment(past, past.plusDays(30)));
        enrollmentRepository.enroll(UUID.randomUUID(), publishedCourse, new Enrollment(past, null));
        enrollmentRepository.enroll(
                UUID.randomUUID(), publishedCourse, new Enrollment(past, null), LocalDateTime.now());
        enrollmentRepository.enroll(UUID.randomUUID(), draftCourse, new Enrollment(past, null));

        final CourseMetrics metrics = useCase.execute();

        assertEquals(1, metrics.topCourses().size());
        assertEquals(2L, metrics.mostAccessedCourse().enrollments());
        assertEquals(1, metrics.categoriesCount());
        assertEquals(List.of(new CategoryShare("Gastronomia", new BigDecimal("100.00"))),
                metrics.enrollmentsByCategory());
    }

    @Test
    void shouldNormalizeBlankCategoriesOrderByPercentageDescCategoryAscAndSumExactlyOneHundred() {
        final UUID courseNullCategory = UUID.fromString("00000000-0000-4000-8000-000000000001");
        final UUID courseEmptyCategory = UUID.fromString("00000000-0000-4000-8000-000000000002");
        final UUID courseSpacesCategory = UUID.fromString("00000000-0000-4000-8000-000000000003");
        final UUID courseEcoturismo = UUID.fromString("00000000-0000-4000-8000-000000000004");
        final UUID courseHotelaria = UUID.fromString("00000000-0000-4000-8000-000000000005");

        enrollmentRepository.registerCourse(courseNullCategory, "Curso A", null, CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(courseEmptyCategory, "Curso B", "", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(courseSpacesCategory, "Curso C", "   ", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(courseEcoturismo, "Curso D", "Ecoturismo", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(courseHotelaria, "Curso E", "Hotelaria", CourseStatus.PUBLISHED);

        enrollUsers(courseNullCategory, 1);
        enrollUsers(courseEmptyCategory, 1);
        enrollUsers(courseSpacesCategory, 1);
        enrollUsers(courseEcoturismo, 3);
        enrollUsers(courseHotelaria, 3);

        final CourseMetrics metrics = useCase.execute();

        assertEquals(3, metrics.categoriesCount());
        assertEquals(List.of(
                new CategoryShare("Ecoturismo", new BigDecimal("33.34")),
                new CategoryShare("Hotelaria", new BigDecimal("33.33")),
                new CategoryShare("Sem categoria", new BigDecimal("33.33"))
        ), metrics.enrollmentsByCategory());
        assertEquals(new BigDecimal("100.00"), sumPercentages(metrics.enrollmentsByCategory()));
    }

    private void enrollUsers(UUID courseId, int quantity) {
        for (int index = 0; index < quantity; index++) {
            enrollmentRepository.enroll(UUID.randomUUID(), courseId, new Enrollment(LocalDate.now(), null));
        }
    }

    private static BigDecimal sumPercentages(List<CategoryShare> shares) {
        return shares.stream()
                .map(CategoryShare::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

