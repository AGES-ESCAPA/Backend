package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.CoursePrerequisiteCycles;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoursePrerequisiteCyclesTest {

    @Test
    void shouldDetectCyclesThroughPublishedDraftPrerequisites() {
        final InMemoryCourseRepositoryPort courses = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisites =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final Course first = courses.save(course("Curso A"));
        final Course second = courses.save(course("Curso B"));
        second.setStatus(CourseStatus.PUBLISHED);
        second.setPendingDraft(CoursePendingDraft.fromPublished(second, List.of(first.getId())));
        courses.save(second);

        final CoursePrerequisiteCycles cycles = new CoursePrerequisiteCycles(courses, prerequisites);

        assertTrue(cycles.createsCycle(first.getId(), second.getId()));
        assertFalse(cycles.createsCycle(first.getId(), UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class,
                () -> cycles.assertAcyclic(first.getId(), List.of(first.getId())));
    }

    @Test
    void shouldStopAtVisitedCoursesAndCheckPersistedPrerequisites() {
        final InMemoryCourseRepositoryPort courses = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisites =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final UUID startId = UUID.randomUUID();
        final UUID firstId = UUID.randomUUID();
        final UUID secondId = UUID.randomUUID();
        prerequisites.save(firstId, secondId);
        prerequisites.save(secondId, firstId);

        final CoursePrerequisiteCycles cycles = new CoursePrerequisiteCycles(courses, prerequisites);

        assertFalse(cycles.createsCycle(startId, firstId));
        prerequisites.save(secondId, startId);
        assertThrows(IllegalArgumentException.class,
                () -> cycles.assertAcyclic(startId, List.of(firstId)));
    }

    @Test
    void shouldAcceptAnAcyclicEmptyPrerequisiteSet() {
        final CoursePrerequisiteCycles cycles = new CoursePrerequisiteCycles(
                new InMemoryCourseRepositoryPort(), new InMemoryCoursePrerequisiteRepositoryPort());

        cycles.assertAcyclic(UUID.randomUUID(), null);
    }

    private static Course course(String title) {
        final User admin = new User("Admin", "admin@example.com", "hash", "ADMIN");
        return new Course(title, null, null, null, null, admin, null, null,
                null, null, null, List.of(), true, false, admin);
    }
}
