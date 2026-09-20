package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetStudentLessonUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final InMemoryLessonRepositoryPort lessonRepository = new InMemoryLessonRepositoryPort();
    private final InMemoryEnrollmentRepositoryPort enrollmentRepository = new InMemoryEnrollmentRepositoryPort();
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private final GetStudentLessonUseCase useCase =
            new GetStudentLessonUseCase(lessonRepository, enrollmentRepository, clock);

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private Content givenLesson(boolean isFree, boolean enforceDeadlineBlock) {
        final UUID moduleId = UUID.randomUUID();
        final Content content = new Content(
                UUID.randomUUID(), moduleId, "Aula 2", "Descricao da aula", ContentType.VIDEO,
                "https://cdn.example.com/aula-2.mp4", 12, isFree, 2, null, null);
        lessonRepository.add(new LessonDetails(content, courseId, "Fundamentos", 1, enforceDeadlineBlock));
        return content;
    }

    private void givenEnrollment(LocalDate startDate, LocalDate expirationDate) {
        enrollmentRepository.enroll(userId, courseId, new Enrollment(startDate, expirationDate));
    }

    private LessonAccessDeniedException assertDenied(UUID lessonId) {
        return assertThrows(
                LessonAccessDeniedException.class,
                () -> useCase.execute(userId, courseId, lessonId)
        );
    }

    @Test
    void shouldReturnLessonWithModuleDataForEnrolledStudent() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        final LessonDetails result = useCase.execute(userId, courseId, lesson.getId());

        assertEquals(lesson.getId(), result.content().getId());
        assertEquals("Aula 2", result.content().getTitle());
        assertEquals("Descricao da aula", result.content().getDescription());
        assertEquals("Fundamentos", result.moduleTitle());
        assertEquals(1, result.moduleOrder());
    }

    @Test
    void shouldAllowFreeLessonWithoutEnrollment() {
        final Content lesson = givenLesson(true, true);

        final LessonDetails result = useCase.execute(userId, courseId, lesson.getId());

        assertEquals(lesson.getId(), result.content().getId());
    }

    @Test
    void shouldAllowFreeLessonEvenWhenEnrollmentExpiredWithDeadlineBlock() {
        final Content lesson = givenLesson(true, true);
        givenEnrollment(TODAY.minusDays(90), TODAY.minusDays(1));

        final LessonDetails result = useCase.execute(userId, courseId, lesson.getId());

        assertEquals(lesson.getId(), result.content().getId());
    }

    @Test
    void shouldDenyPaidLessonWithoutEnrollment() {
        final Content lesson = givenLesson(false, false);

        assertEquals(Reason.NOT_ENROLLED, assertDenied(lesson.getId()).getReason());
    }

    @Test
    void shouldDenyPaidLessonWhenEnrollmentBelongsToAnotherStudent() {
        final Content lesson = givenLesson(false, false);
        enrollmentRepository.enroll(UUID.randomUUID(), courseId, new Enrollment(TODAY.minusDays(1), null));

        assertEquals(Reason.NOT_ENROLLED, assertDenied(lesson.getId()).getReason());
    }

    @Test
    void shouldDenyWhenEnrollmentHasNotStartedYet() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.plusDays(1), TODAY.plusDays(90));

        assertEquals(Reason.ENROLLMENT_NOT_STARTED, assertDenied(lesson.getId()).getReason());
    }

    @Test
    void shouldAllowWhenEnrollmentStartsToday() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY, TODAY.plusDays(90));

        assertEquals(lesson.getId(), useCase.execute(userId, courseId, lesson.getId()).content().getId());
    }

    @Test
    void shouldDenyWhenEnrollmentExpiredAndCourseBlocksByDeadline() {
        final Content lesson = givenLesson(false, true);
        givenEnrollment(TODAY.minusDays(90), TODAY.minusDays(1));

        assertEquals(Reason.ACCESS_EXPIRED, assertDenied(lesson.getId()).getReason());
    }

    @Test
    void shouldAllowExpiredEnrollmentWhenCourseDoesNotBlockByDeadline() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(90), TODAY.minusDays(1));

        assertEquals(lesson.getId(), useCase.execute(userId, courseId, lesson.getId()).content().getId());
    }

    @Test
    void shouldAllowOnTheLastDayOfTheAccessPeriod() {
        final Content lesson = givenLesson(false, true);
        givenEnrollment(TODAY.minusDays(90), TODAY);

        assertEquals(lesson.getId(), useCase.execute(userId, courseId, lesson.getId()).content().getId());
    }

    @Test
    void shouldAllowEnrollmentWithoutExpirationDate() {
        final Content lesson = givenLesson(false, true);
        givenEnrollment(TODAY.minusDays(10), null);

        assertEquals(lesson.getId(), useCase.execute(userId, courseId, lesson.getId()).content().getId());
    }

    @Test
    void shouldRejectUnknownLesson() {
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(userId, courseId, UUID.randomUUID())
        );
    }

    @Test
    void shouldRejectLessonBelongingToAnotherCourse() {
        final Content lesson = givenLesson(true, false);

        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(userId, UUID.randomUUID(), lesson.getId())
        );
    }

    @Test
    void shouldReportNotFoundBeforeAccessDenied() {
        // Sem matricula e sem a aula existir: o 404 vem primeiro, sem revelar regras de acesso.
        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(userId, courseId, UUID.randomUUID())
        );
    }
}
