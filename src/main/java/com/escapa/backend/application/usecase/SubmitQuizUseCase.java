package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.escapa.backend.application.course.PublishedCurriculum;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.QuizSubmitResult;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.EnrollmentProgressRepositoryPort;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.application.port.ModulePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.QuizOption;
import com.escapa.backend.domain.content.QuizQuestion;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Module;

/**
 * Corrige e conclui uma aula do tipo QUIZ quando o aluno atinge o percentual minimo.
 */
public class SubmitQuizUseCase {

    private final LessonRepositoryPort lessonRepositoryPort;
    private final LessonProgressRepositoryPort lessonProgressRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;
    private final EnrollmentProgressRepositoryPort enrollmentProgressRepositoryPort;
    private final EnrollmentRepositoryPort enrollmentRepositoryPort;
    private final EnrollmentAccessValidator enrollmentAccessValidator;
    private final SequentialLessonAccessValidator sequentialLessonAccessValidator;
    private final Clock clock;

    public SubmitQuizUseCase(
            LessonRepositoryPort lessonRepositoryPort,
            LessonProgressRepositoryPort lessonProgressRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort,
            EnrollmentProgressRepositoryPort enrollmentProgressRepositoryPort,
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            CourseRepositoryPort courseRepositoryPort,
            ModulePrerequisiteRepositoryPort modulePrerequisiteRepositoryPort,
            Clock clock
    ) {
        this.lessonRepositoryPort = lessonRepositoryPort;
        this.lessonProgressRepositoryPort = lessonProgressRepositoryPort;
        this.moduleRepositoryPort = moduleRepositoryPort;
        this.enrollmentProgressRepositoryPort = enrollmentProgressRepositoryPort;
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
        this.enrollmentAccessValidator = new EnrollmentAccessValidator(enrollmentRepositoryPort, clock);
        this.sequentialLessonAccessValidator = new SequentialLessonAccessValidator(
                courseRepositoryPort,
                moduleRepositoryPort,
                lessonProgressRepositoryPort,
                modulePrerequisiteRepositoryPort);
        this.clock = clock;
    }

    public QuizSubmitResult execute(
            UUID userId,
            UUID courseId,
            UUID lessonId,
            List<QuizAnswer> answers
    ) {
        final LessonDetails lesson = lessonRepositoryPort.findByCourseIdAndId(courseId, lessonId)
                .orElseThrow(() -> new ContentNotFoundException(lessonId));

        if (lesson.content().getType() != ContentType.QUIZ) {
            throw new IllegalArgumentException("Lesson is not a quiz");
        }

        final boolean isFree = Boolean.TRUE.equals(lesson.content().getIsFree());
        if (!isFree) {
            enrollmentAccessValidator.requireActiveEnrollment(
                    userId, lesson.courseId(), lesson.courseEnforcesDeadlineBlock(), lesson.content().getId());
            sequentialLessonAccessValidator.requireUnlocked(userId, courseId, lessonId);
        } else if (enrollmentRepositoryPort.findByUserIdAndCourseId(userId, courseId).isPresent()) {
            sequentialLessonAccessValidator.requireUnlocked(userId, courseId, lessonId);
        }

        final List<QuizQuestion> questions = lesson.content().getQuizQuestions();
        final int totalQuestions = questions.size();
        if (totalQuestions == 0) {
            throw new IllegalStateException("Quiz lesson has no questions");
        }

        validateAnswers(questions, answers);

        final int correctCount = countCorrectAnswers(questions, answers);
        final int scorePercent = correctCount * 100 / totalQuestions;
        final boolean passed = scorePercent >= lesson.minQuizPassPercentage();

        if (passed) {
            completeLessonAndUpdateEnrollment(userId, courseId, lessonId);
        }

        return new QuizSubmitResult(scorePercent, passed, correctCount, totalQuestions);
    }

    private void completeLessonAndUpdateEnrollment(UUID userId, UUID courseId, UUID lessonId) {
        lessonProgressRepositoryPort.markCompleted(userId, lessonId, LocalDateTime.now(clock));
        final int progress = calculateProgress(userId, courseId);
        enrollmentProgressRepositoryPort.updateProgress(
                userId, courseId, progress, progress == 100 ? LocalDate.now(clock) : null);
    }

    private static int countCorrectAnswers(List<QuizQuestion> questions, List<QuizAnswer> answers) {
        final Map<UUID, UUID> correctOptionByQuestion = correctAnswers(questions);
        int correctCount = 0;
        for (final QuizAnswer answer : answers) {
            final UUID correctOption = correctOptionByQuestion.get(answer.questionId());
            if (correctOption != null && correctOption.equals(answer.optionId())) {
                correctCount++;
            }
        }
        return correctCount;
    }

    private static void validateAnswers(List<QuizQuestion> questions, List<QuizAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("All quiz questions must be answered");
        }
        final Set<UUID> expected = new HashSet<>();
        final Map<UUID, Set<UUID>> optionsByQuestion = new HashMap<>();
        for (final QuizQuestion question : questions) {
            expected.add(question.id());
            final Set<UUID> optionIds = new HashSet<>();
            for (final QuizOption option : question.options()) {
                optionIds.add(option.id());
            }
            optionsByQuestion.put(question.id(), optionIds);
        }

        final Set<UUID> answeredQuestions = new HashSet<>();
        for (final QuizAnswer answer : answers) {
            if (answer.questionId() == null || answer.optionId() == null) {
                throw new IllegalArgumentException("Each answer must include questionId and optionId");
            }
            if (!expected.contains(answer.questionId())) {
                throw new IllegalArgumentException("Unknown questionId: " + answer.questionId());
            }
            if (!answeredQuestions.add(answer.questionId())) {
                throw new IllegalArgumentException("Duplicate answer for questionId: " + answer.questionId());
            }
            if (!optionsByQuestion.get(answer.questionId()).contains(answer.optionId())) {
                throw new IllegalArgumentException("Unknown optionId for question: " + answer.questionId());
            }
        }

        if (answeredQuestions.size() != expected.size()) {
            throw new IllegalArgumentException("All quiz questions must be answered");
        }
    }

    private static Map<UUID, UUID> correctAnswers(List<QuizQuestion> questions) {
        final Map<UUID, UUID> correct = new HashMap<>();
        for (final QuizQuestion question : questions) {
            for (final QuizOption option : question.options()) {
                if (option.correct()) {
                    correct.put(question.id(), option.id());
                    break;
                }
            }
        }
        return correct;
    }

    private int calculateProgress(UUID userId, UUID courseId) {
        final var modules = moduleRepositoryPort.findByCourseId(courseId);
        final boolean published = modules.stream().anyMatch(Module::isCoursePublished);
        final int totalLessons = PublishedCurriculum.forStudents(
                        modules, published ? CourseStatus.PUBLISHED : CourseStatus.DRAFT).stream()
                .mapToInt(module -> module.getContents().size())
                .sum();
        if (totalLessons == 0) {
            return 0;
        }
        final int completedLessons = lessonProgressRepositoryPort.findCompletedLessonIds(userId, courseId).size();
        return Math.min(100, completedLessons * 100 / totalLessons);
    }

    public record QuizAnswer(UUID questionId, UUID optionId) {
    }
}