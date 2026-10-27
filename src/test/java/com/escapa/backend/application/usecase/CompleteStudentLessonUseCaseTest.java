package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;
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
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryEnrollmentProgressRepositoryPort enrollmentProgressRepository =
            new InMemoryEnrollmentProgressRepositoryPort();
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final CompleteStudentLessonUseCase useCase = new CompleteStudentLessonUseCase(
            lessonRepository, progressRepository, moduleRepository, enrollmentProgressRepository,
            enrollmentRepository, clock);

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private Content givenLesson(boolean isFree, boolean enforceDeadlineBlock) {
        final UUID moduleId = moduleRepository.findByCourseId(courseId).stream()
                .findFirst()
                .map(Module::getId)
                .orElseGet(() -> moduleRepository.createModule(courseId));
        final Module module = moduleRepository.findById(moduleId).orElseThrow();
        final Content content = new Content(
                UUID.randomUUID(), moduleId, "Aula", "Descricao", ContentType.VIDEO,
                "https://cdn.example.com/aula.mp4", 10, isFree, module.getContents().size() + 1, null, null);
        module.getContents().add(content);
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
    void shouldUpdateEnrollmentProgressRoundingDown() {
        final Content first = givenLesson(false, false);
        givenLesson(false, false);
        givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        useCase.execute(userId, courseId, first.getId());

        assertEquals(33, enrollmentProgressRepository.progress(userId, courseId));
        assertNull(enrollmentProgressRepository.conclusionDate(userId, courseId));
    }

    @Test
    void shouldConcludeCourseWhenLastLessonIsCompleted() {
        final Content first = givenLesson(false, false);
        final Content second = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        useCase.execute(userId, courseId, first.getId());
        useCase.execute(userId, courseId, second.getId());

        assertEquals(100, enrollmentProgressRepository.progress(userId, courseId));
        assertEquals(TODAY, enrollmentProgressRepository.conclusionDate(userId, courseId));
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
        assertNull(enrollmentProgressRepository.progress(userId, courseId));
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
