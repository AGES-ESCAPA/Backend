package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompleteStudentLessonUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final InMemoryLessonRepositoryPort lessonRepository = new InMemoryLessonRepositoryPort();
    private final InMemoryEnrollmentRepositoryPort enrollmentRepository = new InMemoryEnrollmentRepositoryPort();
    private final InMemoryLessonProgressRepositoryPort progressRepository = new InMemoryLessonProgressRepositoryPort();
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final CompleteStudentLessonUseCase useCase =
            new CompleteStudentLessonUseCase(lessonRepository, progressRepository, enrollmentRepository, clock);

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private Content givenLesson(boolean isFree, boolean enforceDeadlineBlock) {
        final Content content = new Content(
                UUID.randomUUID(), UUID.randomUUID(), "Aula 1", "Descricao", ContentType.VIDEO,
                "https://cdn.example.com/aula-1.mp4", 10, isFree, 1, null, null);
        lessonRepository.add(new LessonDetails(content, courseId, "Fundamentos", 1, enforceDeadlineBlock,
                new LessonSupplement(List.of(), List.of())));
        return content;
    }

    private void givenEnrollment(LocalDate startDate, LocalDate expirationDate) {
        enrollmentRepository.enroll(userId, courseId, new Enrollment(startDate, expirationDate));
    }

    @Test
    void shouldMarkLessonAsCompletedForEnrolledStudent() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(TODAY.atStartOfDay(), progressRepository.completedAt(userId, lesson.getId()));
    }

    @Test
    void shouldKeepOriginalCompletionDateWhenCompletingAgain() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));
        final LocalDateTime firstCompletion = TODAY.minusDays(3).atStartOfDay();
        progressRepository.markCompleted(userId, lesson.getId(), firstCompletion);

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(firstCompletion, progressRepository.completedAt(userId, lesson.getId()));
    }

    @Test
    void shouldAllowCompletingFreeLessonWithoutEnrollment() {
        final Content lesson = givenLesson(true, false);

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(TODAY.atStartOfDay(), progressRepository.completedAt(userId, lesson.getId()));
    }

    @Test
    void shouldDenyPaidLessonWithoutEnrollment() {
        final Content lesson = givenLesson(false, false);

        final LessonAccessDeniedException error = assertThrows(
                LessonAccessDeniedException.class,
                () -> useCase.execute(userId, courseId, lesson.getId()));

        assertEquals(Reason.NOT_ENROLLED, error.getReason());
        assertNull(progressRepository.completedAt(userId, lesson.getId()));
    }

    @Test
    void shouldDenyWhenAccessExpiredOnCourseWithDeadlineBlock() {
        final Content lesson = givenLesson(false, true);
        givenEnrollment(TODAY.minusDays(90), TODAY.minusDays(1));

        final LessonAccessDeniedException error = assertThrows(
                LessonAccessDeniedException.class,
                () -> useCase.execute(userId, courseId, lesson.getId()));

        assertEquals(Reason.ACCESS_EXPIRED, error.getReason());
    }

    @Test
    void shouldRejectLessonFromAnotherCourse() {
        final Content lesson = givenLesson(true, false);

        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(userId, UUID.randomUUID(), lesson.getId()));
    }
}
