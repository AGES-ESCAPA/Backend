package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonConcept;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.LessonReference;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.application.port.LessonSupplementRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;

class OpenStudentLessonUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final InMemoryLessonRepositoryPort lessonRepository = new InMemoryLessonRepositoryPort();
    private final InMemoryEnrollmentRepositoryPort enrollmentRepository = new InMemoryEnrollmentRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryLessonProgressRepositoryPort progressRepository = new InMemoryLessonProgressRepositoryPort();

    private final Map<UUID, LessonSupplement> supplementsDb = new HashMap<>();
    private final LessonSupplementRepositoryPort lessonSupplementRepository = lessonId ->
            supplementsDb.getOrDefault(lessonId, new LessonSupplement(List.of(), List.of()));

    private final Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final OpenStudentLessonUseCase useCase = new OpenStudentLessonUseCase(
            lessonRepository,
            enrollmentRepository,
            lessonSupplementRepository,
            courseRepository,
            moduleRepository,
            progressRepository,
            moduleId -> List.of(),
            clock);

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private Content givenLesson(boolean isFree, boolean enforceDeadlineBlock) {
        final UUID moduleId = UUID.randomUUID();
        final Content content = new Content(
                UUID.randomUUID(), moduleId, "Aula 2", "Descricao da aula", ContentType.VIDEO,
                "https://cdn.example.com/aula-2.mp4", 12, isFree, 2, null, null, null, null, null, false, false);
        lessonRepository.add(new LessonDetails(content, courseId, "Fundamentos", 1, enforceDeadlineBlock, 70,
                new LessonSupplement(List.of(), List.of())));
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
    void shouldRecordLastAccessOnPaidLesson() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(Optional.of(TODAY.atStartOfDay()), enrollmentRepository.lastAccessOf(userId, courseId));
    }

    @Test
    void shouldRecordLastAccessOnFreeLessonWhenEnrolled() {
        final Content lesson = givenLesson(true, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(Optional.of(TODAY.atStartOfDay()), enrollmentRepository.lastAccessOf(userId, courseId));
    }

    @Test
    void shouldNotRecordNorCreateEnrollmentOnFreeLessonWithoutEnrollment() {
        final Content lesson = givenLesson(true, false);

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(Optional.empty(), enrollmentRepository.lastAccessOf(userId, courseId));
        assertEquals(Optional.empty(), enrollmentRepository.findByUserIdAndCourseId(userId, courseId));
    }

    @Test
    void shouldNotRecordLastAccessWhenAccessIsDenied() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.plusDays(1), TODAY.plusDays(90));

        assertDenied(lesson.getId());

        assertEquals(Optional.empty(), enrollmentRepository.lastAccessOf(userId, courseId));
    }

    @Test
    void shouldNotRecordLastAccessWhenEnrollmentExpiredAndCourseBlocksByDeadline() {
        final Content lesson = givenLesson(false, true);
        givenEnrollment(TODAY.minusDays(90), TODAY.minusDays(1));

        assertDenied(lesson.getId());

        assertEquals(Optional.empty(), enrollmentRepository.lastAccessOf(userId, courseId));
    }

    @Test
    void shouldNotRecordLastAccessOnFreeLessonWhenEnrollmentIsRevoked() {
        final Content lesson = givenLesson(true, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));
        enrollmentRepository.revoke(userId, courseId);

        assertEquals(lesson.getId(), useCase.execute(userId, courseId, lesson.getId()).content().getId());

        assertEquals(Optional.empty(), enrollmentRepository.lastAccessOf(userId, courseId));
    }

    @Test
    void shouldNotOverwriteLastAccessRecordedWithinTheMinimumInterval() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));
        final LocalDateTime recent = TODAY.atStartOfDay().minusSeconds(30);
        enrollmentRepository.recordLastAccess(userId, courseId, recent, Duration.ZERO);

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(Optional.of(recent), enrollmentRepository.lastAccessOf(userId, courseId));
    }

    @Test
    void shouldRefreshLastAccessOlderThanTheMinimumInterval() {
        final Content lesson = givenLesson(false, false);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));
        enrollmentRepository.recordLastAccess(
                userId, courseId, TODAY.atStartOfDay().minusMinutes(2), Duration.ZERO);

        useCase.execute(userId, courseId, lesson.getId());

        assertEquals(Optional.of(TODAY.atStartOfDay()), enrollmentRepository.lastAccessOf(userId, courseId));
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

    @Test
    void shouldReturnLessonWithConceptsAndReferences() {
        final Content lesson = givenLesson(true, false);
        
        List<LessonConcept> concepts = List.of(
            new LessonConcept("Conceito A", 1),
            new LessonConcept("Conceito B", 2)
        );
        List<LessonReference> references = List.of(
            new LessonReference(UUID.randomUUID(), "Nome da fonte", "https://exemplo.com/artigo", 1)
        );
        supplementsDb.put(lesson.getId(), new LessonSupplement(concepts, references));

        final LessonDetails result = useCase.execute(userId, courseId, lesson.getId());

        assertEquals(lesson.getId(), result.content().getId());
        assertEquals(concepts, result.supplement().concepts());
        assertEquals(references, result.supplement().references());
    }

    @Test
    void shouldReturnLessonWithEmptyConceptsAndReferences() {
        final Content lesson = givenLesson(true, false);

        final LessonDetails result = useCase.execute(userId, courseId, lesson.getId());

        assertEquals(lesson.getId(), result.content().getId());
        assertEquals(0, result.supplement().concepts().size());
        assertEquals(0, result.supplement().references().size());
    }

    @Test
    void shouldDenyLockedLessonWhenSequentialProgressIsRequired() {
        final Content second = givenSequentialLessons().get(1);
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));

        assertEquals(Reason.SEQUENTIAL_PROGRESS_LOCKED, assertDenied(second.getId()).getReason());
    }

    @Test
    void shouldAllowNextLessonAfterPreviousIsCompletedWithSequentialProgress() {
        final List<Content> lessons = givenSequentialLessons();
        givenEnrollment(TODAY.minusDays(10), TODAY.plusDays(90));
        progressRepository.markCompleted(userId, lessons.get(0).getId(), TODAY.atStartOfDay());

        final LessonDetails result = useCase.execute(userId, courseId, lessons.get(1).getId());

        assertEquals(lessons.get(1).getId(), result.content().getId());
    }

    private List<Content> givenSequentialLessons() {
        final Course course = new Course();
        course.setId(courseId);
        course.setRequireSequentialProgress(true);
        courseRepository.save(course);

        final UUID moduleId = moduleRepository.createModule(courseId);
        final Module module = moduleRepository.findById(moduleId).orElseThrow();
        final Content first = new Content(
                UUID.randomUUID(), moduleId, "Aula 1", "Descricao", ContentType.VIDEO,
                "https://cdn.example.com/aula-1.mp4", 10, false, 1, null, null, null, null, null, false, false);
        final Content second = new Content(
                UUID.randomUUID(), moduleId, "Aula 2", "Descricao", ContentType.VIDEO,
                "https://cdn.example.com/aula-2.mp4", 10, false, 2, null, null, null, null, null, false, false);
        module.getContents().add(first);
        module.getContents().add(second);
        moduleRepository.save(module);

        lessonRepository.add(new LessonDetails(
                first, courseId, "Fundamentos", 1, false, 70, new LessonSupplement(List.of(), List.of())));
        lessonRepository.add(new LessonDetails(
                second, courseId, "Fundamentos", 1, false, 70, new LessonSupplement(List.of(), List.of())));
        return List.of(first, second);
    }
}
