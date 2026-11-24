package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;

class SequentialLessonAccessValidatorTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private InMemoryCourseRepositoryPort courses;
    private InMemoryModuleRepositoryPort modules;
    private InMemoryLessonProgressRepositoryPort progress;
    private SequentialLessonAccessValidator validator;

    @BeforeEach
    void setUp() {
        courses = new InMemoryCourseRepositoryPort();
        modules = new InMemoryModuleRepositoryPort();
        progress = new InMemoryLessonProgressRepositoryPort();
        validator = new SequentialLessonAccessValidator(
                courses, modules, progress, moduleId -> List.of());
    }

    @Test
    void shouldAllowWhenCourseDoesNotRequireSequentialProgress() {
        final Course course = new Course();
        course.setId(courseId);
        course.setRequireSequentialProgress(false);
        courses.save(course);

        assertDoesNotThrow(() -> validator.requireUnlocked(userId, courseId, UUID.randomUUID()));
    }

    @Test
    void shouldAllowWhenCourseIsMissing() {
        assertDoesNotThrow(() -> validator.requireUnlocked(userId, courseId, UUID.randomUUID()));
    }

    @Test
    void shouldDenyLockedLessonWhenPreviousIsIncomplete() {
        final List<Content> lessons = givenSequentialLessons(true);

        final LessonAccessDeniedException error = assertThrows(
                LessonAccessDeniedException.class,
                () -> validator.requireUnlocked(userId, courseId, lessons.get(1).getId()));

        assertEquals(Reason.SEQUENTIAL_PROGRESS_LOCKED, error.getReason());
        assertTrue(error.getMessage().contains(lessons.get(1).getId().toString()));
    }

    @Test
    void shouldAllowNextLessonAfterPreviousIsCompleted() {
        final List<Content> lessons = givenSequentialLessons(true);
        progress.markCompleted(userId, lessons.get(0).getId(), LocalDateTime.of(2026, 6, 15, 10, 0));

        assertDoesNotThrow(() -> validator.requireUnlocked(userId, courseId, lessons.get(1).getId()));
    }

    @Test
    void shouldAllowUnknownLessonIdWithoutThrowing() {
        givenSequentialLessons(true);

        assertDoesNotThrow(() -> validator.requireUnlocked(userId, courseId, UUID.randomUUID()));
    }

    @Test
    void shouldAllowWhenRequireSequentialProgressIsNull() {
        final Course course = new Course();
        course.setId(courseId);
        course.setRequireSequentialProgress(null);
        courses.save(course);

        assertDoesNotThrow(() -> validator.requireUnlocked(userId, courseId, UUID.randomUUID()));
    }

    private List<Content> givenSequentialLessons(boolean requireSequential) {
        final Course course = new Course();
        course.setId(courseId);
        course.setRequireSequentialProgress(requireSequential);
        courses.save(course);

        final UUID moduleId = modules.createModule(courseId);
        final Module module = modules.findById(moduleId).orElseThrow();
        final Content first = new Content(
                UUID.randomUUID(), moduleId, "Aula 1", "Descricao", ContentType.VIDEO,
                "https://cdn.example.com/aula-1.mp4", 10, false, 1, null, null, null, null, null, false, false);
        final Content second = new Content(
                UUID.randomUUID(), moduleId, "Aula 2", "Descricao", ContentType.VIDEO,
                "https://cdn.example.com/aula-2.mp4", 10, false, 2, null, null, null, null, null, false, false);
        module.getContents().add(first);
        module.getContents().add(second);
        modules.save(module);
        return List.of(first, second);
    }
}
