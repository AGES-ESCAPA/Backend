package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.course.CoursePrerequisiteCycles;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

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
    }

    @Test
    void shouldAcceptExistingCourseWhosePrerequisitesDoNotLeadBack() {
        final InMemoryCourseRepositoryPort courses = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisites =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final Course origin = courses.save(course("Origem"));
        final Course other = courses.save(course("Outro"));
        final Course leaf = courses.save(course("Folha"));
        final Course candidate = courses.save(course("Candidato"));
        leaf.setStatus(CourseStatus.PUBLISHED);
        leaf.setPendingDraft(CoursePendingDraft.fromPublished(leaf, List.of(other.getId())));
        courses.save(leaf);
        candidate.setStatus(CourseStatus.PUBLISHED);
        candidate.setPendingDraft(CoursePendingDraft.fromPublished(candidate, List.of(leaf.getId())));
        courses.save(candidate);

        final CoursePrerequisiteCycles cycles = new CoursePrerequisiteCycles(courses, prerequisites);

        assertFalse(cycles.createsCycle(origin.getId(), candidate.getId()));
    }

    @Test
    void shouldRejectCourseAsItsOwnPrerequisite() {
        final InMemoryCourseRepositoryPort courses = new InMemoryCourseRepositoryPort();
        final Course first = courses.save(course("Curso A"));
        final CoursePrerequisiteCycles cycles = new CoursePrerequisiteCycles(
                courses, new InMemoryCoursePrerequisiteRepositoryPort());

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cycles.assertAcyclic(first.getId(), List.of(first.getId())));

        assertEquals("O pré-requisito formaria um ciclo.", exception.getMessage());
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
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cycles.assertAcyclic(startId, List.of(firstId)));
        assertEquals("O pré-requisito formaria um ciclo.", exception.getMessage());
    }

    @Test
    void shouldAcceptAnAcyclicPrerequisiteChain() {
        final InMemoryCourseRepositoryPort courses = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisites =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final Course courseA = courses.save(course("Curso A"));
        final Course courseB = courses.save(course("Curso B"));
        final Course courseC = courses.save(course("Curso C"));
        prerequisites.save(courseB.getId(), courseC.getId());
        final CoursePrerequisiteCycles cycles = new CoursePrerequisiteCycles(courses, prerequisites);

        assertDoesNotThrow(() -> cycles.assertAcyclic(courseA.getId(), List.of(courseB.getId())));
    }

    private static Course course(String title) {
        final User admin = new User("Admin", "admin@example.com", "hash", "ADMIN");
        return new Course(title, null, null, null, null, admin, null, null,
                null, null, null, List.of(), true, false, admin);
    }
}
