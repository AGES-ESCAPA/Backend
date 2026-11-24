package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;

class EnrollmentAccessValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();

    private InMemoryEnrollmentRepositoryPort enrollments;
    private EnrollmentAccessValidator validator;

    @BeforeEach
    void setUp() {
        enrollments = new InMemoryEnrollmentRepositoryPort();
        validator = new EnrollmentAccessValidator(enrollments, CLOCK);
    }

    @Test
    void shouldAllowActiveEnrollmentWithoutDeadlineBlock() {
        enrollments.enroll(userId, courseId, new Enrollment(TODAY.minusDays(1), TODAY.minusDays(1)));

        assertDoesNotThrow(() -> validator.requireActiveEnrollment(userId, courseId, false, resourceId));
    }

    @Test
    void shouldAllowAccessOnTheExpirationDayWhenDeadlineIsEnforced() {
        enrollments.enroll(userId, courseId, new Enrollment(TODAY.minusDays(10), TODAY));

        assertDoesNotThrow(() -> validator.requireActiveEnrollment(userId, courseId, true, resourceId));
    }

    @Test
    void shouldDenyWhenStudentIsNotEnrolled() {
        final LessonAccessDeniedException error = assertThrows(
                LessonAccessDeniedException.class,
                () -> validator.requireActiveEnrollment(userId, courseId, true, resourceId));

        assertEquals(Reason.NOT_ENROLLED, error.getReason());
        assertTrue(error.getMessage().contains(resourceId.toString()));
    }

    @Test
    void shouldDenyWhenEnrollmentHasNotStarted() {
        enrollments.enroll(userId, courseId, new Enrollment(TODAY.plusDays(1), TODAY.plusDays(30)));

        final LessonAccessDeniedException error = assertThrows(
                LessonAccessDeniedException.class,
                () -> validator.requireActiveEnrollment(userId, courseId, false, resourceId));

        assertEquals(Reason.ENROLLMENT_NOT_STARTED, error.getReason());
    }

    @Test
    void shouldDenyWhenAccessExpiredAndDeadlineIsEnforced() {
        enrollments.enroll(userId, courseId, new Enrollment(TODAY.minusDays(30), TODAY.minusDays(1)));

        final LessonAccessDeniedException error = assertThrows(
                LessonAccessDeniedException.class,
                () -> validator.requireActiveEnrollment(userId, courseId, true, resourceId));

        assertEquals(Reason.ACCESS_EXPIRED, error.getReason());
    }

    @Test
    void shouldIgnoreNullEnrollmentDates() {
        enrollments.enroll(userId, courseId, new Enrollment(null, null));

        assertDoesNotThrow(() -> validator.requireActiveEnrollment(userId, courseId, true, resourceId));
    }
}
