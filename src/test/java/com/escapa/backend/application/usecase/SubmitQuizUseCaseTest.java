package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.QuizOption;
import com.escapa.backend.domain.content.QuizQuestion;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;

class SubmitQuizUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final UUID QUESTION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CORRECT_OPTION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID WRONG_OPTION_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

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
        final UUID moduleId = moduleRepository.findByCourseId(courseId).stream()
                .findFirst()
                .map(Module::getId)
                .orElseGet(() -> moduleRepository.createModule(courseId));
        final Module module = moduleRepository.findById(moduleId).orElseThrow();
        final List<QuizQuestion> questions = List.of(new QuizQuestion(
                QUESTION_ID,
                "Pergunta",
                List.of(
                        new QuizOption(CORRECT_OPTION_ID, "Correta", true),
                        new QuizOption(WRONG_OPTION_ID, "Errada", false))));
        final Content content = new Content();
        content.setId(UUID.randomUUID());
        content.setModuleId(moduleId);
        content.setTitle("Quiz");
        content.setDescription("Descricao");
        content.setType(ContentType.QUIZ);
        content.setOrder(module.getContents().size() + 1);
        content.setQuizQuestions(questions);
        module.getContents().add(content);
        moduleRepository.save(module);
        lessonRepository.add(new LessonDetails(
                content, courseId, "Modulo", 1, false, minPassPercent, new LessonSupplement(List.of(), List.of())));
        return content;
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
}