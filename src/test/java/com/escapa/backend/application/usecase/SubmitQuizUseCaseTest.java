package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.QuizOption;
import com.escapa.backend.domain.content.QuizQuestion;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;

class SubmitQuizUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final UUID QUESTION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CORRECT_OPTION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID WRONG_OPTION_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID QUESTION_ID_2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID CORRECT_OPTION_ID_2 = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID WRONG_OPTION_ID_2 = UUID.fromString("20000000-0000-0000-0000-000000000004");

    private final InMemoryLessonRepositoryPort lessonRepository = new InMemoryLessonRepositoryPort();
    private final InMemoryEnrollmentRepositoryPort enrollmentRepository = new InMemoryEnrollmentRepositoryPort();
    private final InMemoryLessonProgressRepositoryPort progressRepository = new InMemoryLessonProgressRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final InMemoryEnrollmentProgressRepositoryPort enrollmentProgressRepository =
            new InMemoryEnrollmentProgressRepositoryPort();
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final SubmitQuizUseCase useCase = new SubmitQuizUseCase(
            lessonRepository,
            progressRepository,
            moduleRepository,
            enrollmentProgressRepository,
            enrollmentRepository,
            courseRepository,
            moduleId -> List.of(),
            clock);

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private Content givenQuizLesson(int minPassPercent) {
        return givenQuizLesson(minPassPercent, false, defaultQuestions());
    }

    private Content givenQuizLesson(int minPassPercent, boolean isFree, List<QuizQuestion> questions) {
        final UUID moduleId = moduleRepository.findByCourseId(courseId).stream()
                .findFirst()
                .map(Module::getId)
                .orElseGet(() -> moduleRepository.createModule(courseId));
        final Module module = moduleRepository.findById(moduleId).orElseThrow();
        final Content content = new Content();
        content.setId(UUID.randomUUID());
        content.setModuleId(moduleId);
        content.setTitle("Quiz");
        content.setDescription("Descricao");
        content.setType(ContentType.QUIZ);
        content.setIsFree(isFree);
        content.setOrder(module.getContents().size() + 1);
        content.setQuizQuestions(questions);
        module.getContents().add(content);
        moduleRepository.save(module);
        lessonRepository.add(new LessonDetails(
                content, courseId, "Modulo", 1, false, minPassPercent, new LessonSupplement(List.of(), List.of())));
        return content;
    }

    private static List<QuizQuestion> defaultQuestions() {
        return List.of(new QuizQuestion(
                QUESTION_ID,
                "Pergunta",
                List.of(
                        new QuizOption(CORRECT_OPTION_ID, "Correta", true),
                        new QuizOption(WRONG_OPTION_ID, "Errada", false))));
    }

    private static List<QuizQuestion> twoQuestions() {
        return List.of(
                new QuizQuestion(
                        QUESTION_ID,
                        "Pergunta 1",
                        List.of(
                                new QuizOption(CORRECT_OPTION_ID, "Correta", true),
                                new QuizOption(WRONG_OPTION_ID, "Errada", false))),
                new QuizQuestion(
                        QUESTION_ID_2,
                        "Pergunta 2",
                        List.of(
                                new QuizOption(CORRECT_OPTION_ID_2, "Correta", true),
                                new QuizOption(WRONG_OPTION_ID_2, "Errada", false))));
    }

    private void givenEnrollment() {
        enrollmentRepository.enroll(userId, courseId, new Enrollment(TODAY.minusDays(10), TODAY.plusDays(90)));
    }

    @Test
    void shouldCompleteLessonWhenQuizIsPassed() {
        final Content quiz = givenQuizLesson(70);
        givenEnrollment();

        final var result = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID)));

        assertTrue(result.passed());
        assertEquals(100, result.scorePercent());
        assertEquals(1, result.correctCount());
        assertEquals(1, result.totalQuestions());
        assertEquals(TODAY.atStartOfDay(), progressRepository.completedAt(userId, quiz.getId()));
        assertEquals(100, enrollmentProgressRepository.progress(userId, courseId));
    }

    @Test
    void shouldAllowRetryWhenQuizFails() {
        final Content quiz = givenQuizLesson(70);
        givenEnrollment();

        final var failed = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, WRONG_OPTION_ID)));

        assertFalse(failed.passed());
        assertEquals(0, failed.scorePercent());
        assertNull(progressRepository.completedAt(userId, quiz.getId()));

        final var passed = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID)));

        assertTrue(passed.passed());
        assertEquals(TODAY.atStartOfDay(), progressRepository.completedAt(userId, quiz.getId()));
    }

    @Test
    void shouldRejectIncompleteOrUnknownAnswers() {
        final Content quiz = givenQuizLesson(70);
        givenEnrollment();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(UUID.randomUUID(), CORRECT_OPTION_ID))));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, UUID.randomUUID()))));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(
                        new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID),
                        new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, WRONG_OPTION_ID))));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(null, CORRECT_OPTION_ID))));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, null))));
    }

    @Test
    void shouldRejectSubmittingANonQuizLesson() {
        final UUID moduleId = moduleRepository.createModule(courseId);
        final Module module = moduleRepository.findById(moduleId).orElseThrow();
        final Content text = new Content();
        text.setId(UUID.randomUUID());
        text.setModuleId(moduleId);
        text.setTitle("Texto");
        text.setDescription("Conteudo");
        text.setType(ContentType.TEXT);
        text.setOrder(1);
        module.getContents().add(text);
        moduleRepository.save(module);
        lessonRepository.add(new LessonDetails(
                text, courseId, "Modulo", 1, false, 70, new LessonSupplement(List.of(), List.of())));
        givenEnrollment();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, text.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID))));
    }

    @Test
    void shouldAllowFreeQuizWithoutEnrollment() {
        final Content quiz = givenQuizLesson(70, true, defaultQuestions());

        final var result = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID)));

        assertTrue(result.passed());
        assertEquals(TODAY.atStartOfDay(), progressRepository.completedAt(userId, quiz.getId()));
    }

    @Test
    void shouldDenyPaidQuizWithoutEnrollment() {
        final Content quiz = givenQuizLesson(70);

        assertThrows(LessonAccessDeniedException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID))));
    }

    @Test
    void shouldThrowWhenLessonNotFound() {
        assertThrows(ContentNotFoundException.class, () -> useCase.execute(
                userId, courseId, UUID.randomUUID(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID))));
    }

    @Test
    void shouldThrowWhenQuizHasNoQuestions() {
        final Content quiz = givenQuizLesson(70, false, List.of());
        givenEnrollment();

        assertThrows(IllegalStateException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID))));
    }

    @Test
    void shouldFailWhenPartialScoreIsBelowPassPercentage() {
        final Content quiz = givenQuizLesson(70, false, twoQuestions());
        givenEnrollment();

        final var result = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(
                        new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID),
                        new SubmitQuizUseCase.QuizAnswer(QUESTION_ID_2, WRONG_OPTION_ID_2)));

        assertFalse(result.passed());
        assertEquals(50, result.scorePercent());
        assertNull(progressRepository.completedAt(userId, quiz.getId()));
    }

    @Test
    void shouldUpdateEnrollmentProgressWithoutCompletionDateWhenCourseIsIncomplete() {
        final Content quiz = givenQuizLesson(70, false, defaultQuestions());
        final UUID moduleId = quiz.getModuleId();
        final Module module = moduleRepository.findById(moduleId).orElseThrow();
        final Content second = new Content();
        second.setId(UUID.randomUUID());
        second.setModuleId(moduleId);
        second.setTitle("Texto");
        second.setDescription("Outra aula");
        second.setType(ContentType.TEXT);
        second.setOrder(2);
        module.getContents().add(second);
        moduleRepository.save(module);
        givenEnrollment();

        useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID)));

        assertEquals(50, enrollmentProgressRepository.progress(userId, courseId));
        assertNull(enrollmentProgressRepository.conclusionDate(userId, courseId));
    }

    @Test
    void shouldStillEnforceSequentialAccessOnFreeQuizWhenStudentIsEnrolled() {
        final Content quiz = givenQuizLesson(70, true, defaultQuestions());
        givenEnrollment();

        final var result = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID)));

        assertTrue(result.passed());
        assertEquals(TODAY.atStartOfDay(), progressRepository.completedAt(userId, quiz.getId()));
    }

    @Test
    void shouldRejectNullAnswersAndPartialAnswerSets() {
        final Content quiz = givenQuizLesson(70, false, twoQuestions());
        givenEnrollment();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(), null));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID))));
    }

    @Test
    void shouldScoreZeroWhenNoOptionIsMarkedCorrect() {
        final Content quiz = givenQuizLesson(70, false, List.of(new QuizQuestion(
                QUESTION_ID,
                "Sem gabarito",
                List.of(
                        new QuizOption(WRONG_OPTION_ID, "A", false),
                        new QuizOption(CORRECT_OPTION_ID, "B", false)))));
        givenEnrollment();

        final var result = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID)));

        assertFalse(result.passed());
        assertEquals(0, result.scorePercent());
        assertEquals(0, result.correctCount());
    }

    @Test
    void shouldCountCorrectAnswerWhenCorrectOptionIsNotFirst() {
        final Content quiz = givenQuizLesson(70, false, List.of(new QuizQuestion(
                QUESTION_ID,
                "Ordem invertida",
                List.of(
                        new QuizOption(WRONG_OPTION_ID, "Errada", false),
                        new QuizOption(CORRECT_OPTION_ID, "Correta", true)))));
        givenEnrollment();
        moduleRepository.findByCourseId(courseId).forEach(module -> {
            module.setCoursePublished(true);
            moduleRepository.save(module);
        });

        final var result = useCase.execute(
                userId, courseId, quiz.getId(),
                List.of(new SubmitQuizUseCase.QuizAnswer(QUESTION_ID, CORRECT_OPTION_ID)));

        assertTrue(result.passed());
        assertEquals(100, result.scorePercent());
    }
}