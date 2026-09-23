package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonStatus;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonStatusCalculatorTest {

    private final LessonStatusCalculator calculator = new LessonStatusCalculator();
    private final UUID courseId = UUID.randomUUID();

    private Content content(UUID moduleId, String title, int order) {
        return new Content(
                UUID.randomUUID(), moduleId, title, "desc", ContentType.VIDEO,
                "https://cdn.example.com/video.mp4", 10, false, order, null, null);
    }

    private Function<UUID, List<UUID>> noPrerequisites() {
        return moduleId -> List.of();
    }

    @Test
    void shouldMarkLessonsWithProgressAsCompleted() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        final Content c2 = content(m1.getId(), "Aula 2", 2);
        m1.getContents().add(c1);
        m1.getContents().add(c2);

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1), List.of(c1.getId()), noPrerequisites(), true);

        final StudentCourseCurriculum.Lesson lesson1 = result.modules().get(0).lessons().get(0);
        final StudentCourseCurriculum.Lesson lesson2 = result.modules().get(0).lessons().get(1);

        assertEquals(LessonStatus.COMPLETED, lesson1.status());
        assertEquals(LessonStatus.AVAILABLE, lesson2.status());
        assertEquals(1, result.completedLessons());
        assertEquals(2, result.totalLessons());
    }

    @Test
    void shouldLockLessonsAfterTheFirstUncompletedOneWhenSequentialProgressIsRequired() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        final Content c2 = content(m1.getId(), "Aula 2", 2);
        final Content c3 = content(m1.getId(), "Aula 3", 3);
        m1.getContents().add(c1);
        m1.getContents().add(c2);
        m1.getContents().add(c3);

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1), List.of(c1.getId()), noPrerequisites(), true);

        final List<StudentCourseCurriculum.Lesson> lessons = result.modules().get(0).lessons();
        assertEquals(LessonStatus.COMPLETED, lessons.get(0).status());
        assertEquals(LessonStatus.AVAILABLE, lessons.get(1).status());
        assertEquals(LessonStatus.LOCKED, lessons.get(2).status());
    }

    @Test
    void firstLessonOfCourseIsNeverLockedBySequentialRule() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        m1.getContents().add(c1);

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1), List.of(), noPrerequisites(), true);

        assertEquals(LessonStatus.AVAILABLE, result.modules().get(0).lessons().get(0).status());
    }

    @Test
    void shouldMakeAllLessonsAvailableWhenSequentialProgressIsDisabled() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        final Content c2 = content(m1.getId(), "Aula 2", 2);
        final Content c3 = content(m1.getId(), "Aula 3", 3);
        m1.getContents().add(c1);
        m1.getContents().add(c2);
        m1.getContents().add(c3);

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1), List.of(), noPrerequisites(), false);

        final List<StudentCourseCurriculum.Lesson> lessons = result.modules().get(0).lessons();
        assertTrue(lessons.stream().allMatch(l -> l.status() == LessonStatus.AVAILABLE));
    }

    @Test
    void shouldLockAllLessonsOfAModuleWithIncompletePrerequisite() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        m1.getContents().add(c1);

        final Module m2 = new Module(UUID.randomUUID(), courseId, "Modulo 2", 2);
        final Content c2 = content(m2.getId(), "Aula 2", 1);
        final Content c3 = content(m2.getId(), "Aula 3", 2);
        m2.getContents().add(c2);
        m2.getContents().add(c3);

        final Function<UUID, List<UUID>> prerequisites = moduleId ->
                moduleId.equals(m2.getId()) ? List.of(m1.getId()) : List.of();

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1, m2), List.of(), prerequisites, false);

        final StudentCourseCurriculum.Module module2Result = result.modules().get(1);
        assertTrue(module2Result.locked());
        assertTrue(module2Result.lessons().stream().allMatch(l -> l.status() == LessonStatus.LOCKED));
    }

    @Test
    void shouldUnlockModuleWhenAllPrerequisiteModulesAreFullyCompleted() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        m1.getContents().add(c1);

        final Module m2 = new Module(UUID.randomUUID(), courseId, "Modulo 2", 2);
        final Content c2 = content(m2.getId(), "Aula 2", 1);
        m2.getContents().add(c2);

        final Function<UUID, List<UUID>> prerequisites = moduleId ->
                moduleId.equals(m2.getId()) ? List.of(m1.getId()) : List.of();

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1, m2), List.of(c1.getId()), prerequisites, false);

        final StudentCourseCurriculum.Module module2Result = result.modules().get(1);
        assertFalse(module2Result.locked());
        assertEquals(LessonStatus.AVAILABLE, module2Result.lessons().get(0).status());
    }

    @Test
    void shouldKeepModulesAndLessonsInAdminDefinedOrder() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        final Content c2 = content(m1.getId(), "Aula 2", 2);
        m1.getContents().add(c1);
        m1.getContents().add(c2);

        final Module m2 = new Module(UUID.randomUUID(), courseId, "Modulo 2", 2);
        final Content c3 = content(m2.getId(), "Aula 3", 1);
        m2.getContents().add(c3);

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1, m2), List.of(), noPrerequisites(), false);

        assertEquals("Modulo 1", result.modules().get(0).title());
        assertEquals("Modulo 2", result.modules().get(1).title());
        assertEquals("Aula 1", result.modules().get(0).lessons().get(0).title());
        assertEquals("Aula 2", result.modules().get(0).lessons().get(1).title());
    }

    @Test
    void shouldComputeCompletedAndTotalCountersPerModuleAndCourse() {
        final Module m1 = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content c1 = content(m1.getId(), "Aula 1", 1);
        final Content c2 = content(m1.getId(), "Aula 2", 2);
        m1.getContents().add(c1);
        m1.getContents().add(c2);

        final Module m2 = new Module(UUID.randomUUID(), courseId, "Modulo 2", 2);
        final Content c3 = content(m2.getId(), "Aula 3", 1);
        m2.getContents().add(c3);

        final StudentCourseCurriculum result = calculator.calculate(
                courseId, List.of(m1, m2), List.of(c1.getId(), c3.getId()), noPrerequisites(), false);

        assertEquals(1, result.modules().get(0).completedLessons());
        assertEquals(2, result.modules().get(0).totalLessons());
        assertEquals(1, result.modules().get(1).completedLessons());
        assertEquals(1, result.modules().get(1).totalLessons());
        assertEquals(2, result.completedLessons());
        assertEquals(3, result.totalLessons());
    }
}