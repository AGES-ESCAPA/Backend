package com.escapa.backend.application.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.usecase.InMemoryCoursePrerequisiteRepositoryPort;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

class PublishedCourseDraftsTest {

    @Test
    void shouldDetectPublishedStatus() {
        final Course published = new Course();
        published.setStatus(CourseStatus.PUBLISHED);
        final Course draft = new Course();
        draft.setStatus(CourseStatus.DRAFT);

        assertTrue(PublishedCourseDrafts.isPublished(published));
        assertFalse(PublishedCourseDrafts.isPublished(draft));
    }

    @Test
    void shouldReuseExistingPendingDraft() {
        final Course course = publishedCourse("Publicado");
        final CoursePendingDraft draft = CoursePendingDraft.fromPublished(course, List.of());
        course.setPendingDraft(draft);

        assertSame(draft, PublishedCourseDrafts.ensure(course, new InMemoryCoursePrerequisiteRepositoryPort()));
    }

    @Test
    void shouldBuildDraftFromPublishedValuesAndPrerequisites() {
        final Course course = publishedCourse("Publicado");
        final UUID prerequisiteId = UUID.randomUUID();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisites =
                new InMemoryCoursePrerequisiteRepositoryPort();
        prerequisites.save(course.getId(), prerequisiteId);

        final CoursePendingDraft draft = PublishedCourseDrafts.ensure(course, prerequisites);

        assertEquals("Publicado", draft.title());
        assertEquals(List.of(prerequisiteId), draft.prerequisiteIds());
        assertNull(course.getPendingDraft());
    }

    @Test
    void shouldReturnSameCourseWhenNotPublishedOrWithoutDraft() {
        final Course draftCourse = new Course();
        draftCourse.setStatus(CourseStatus.DRAFT);
        draftCourse.setPendingDraft(CoursePendingDraft.fromPublished(draftCourse, List.of()));

        final Course publishedWithoutDraft = publishedCourse("Sem rascunho");

        assertSame(draftCourse, PublishedCourseDrafts.editorView(draftCourse));
        assertSame(publishedWithoutDraft, PublishedCourseDrafts.editorView(publishedWithoutDraft));
    }

    @Test
    void shouldOverlayPendingDraftKeepingFeaturedAndSameInstructor() {
        final User instructor = instructor("Instrutora", "instrutora@escapa.com");
        final Course course = publishedCourse("Publicado");
        course.setInstructor(instructor);
        course.setFeatured(true);
        course.setPendingDraft(new CoursePendingDraft(
                "Rascunho",
                "Resumo novo",
                "Descricao nova",
                null,
                null,
                instructor.getId(),
                "Marketing",
                "INICIANTE",
                40,
                30,
                99.0,
                List.of("Objetivo"),
                false,
                true,
                80,
                List.of()));

        final Course view = PublishedCourseDrafts.editorView(course);

        assertNotSame(course, view);
        assertEquals("Rascunho", view.getTitle());
        assertEquals("Resumo novo", view.getShortDescription());
        assertEquals("Descricao nova", view.getDescription());
        assertEquals("Marketing", view.getCategory());
        assertFalse(view.getRequireSequentialProgress());
        assertTrue(view.getEnforceDeadlineBlock());
        assertEquals(80, view.getMinQuizPassPercentage());
        assertTrue(view.getFeatured());
        assertSame(instructor, view.getInstructor());
        assertEquals("Publicado", course.getTitle());
    }

    @Test
    void shouldCreatePlaceholderInstructorWhenDraftChangesInstructor() {
        final User publishedInstructor = instructor("Antiga", "antiga@escapa.com");
        final UUID newInstructorId = UUID.randomUUID();
        final Course course = publishedCourse("Publicado");
        course.setInstructor(publishedInstructor);
        course.setPendingDraft(new CoursePendingDraft(
                "Rascunho",
                null,
                null,
                null,
                null,
                newInstructorId,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of()));

        final Course view = PublishedCourseDrafts.editorView(course);

        assertEquals(newInstructorId, view.getInstructor().getId());
        assertNull(view.getInstructor().getName());
        assertSame(publishedInstructor, course.getInstructor());
    }

    @Test
    void shouldLeaveInstructorUnsetWhenDraftHasNoInstructorId() {
        final Course course = publishedCourse("Publicado");
        course.setInstructor(instructor("Instrutora", "instrutora@escapa.com"));
        course.setPendingDraft(new CoursePendingDraft(
                "Rascunho",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of()));

        final Course view = PublishedCourseDrafts.editorView(course);

        assertNull(view.getInstructor());
    }

    private static Course publishedCourse(String title) {
        final Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setTitle(title);
        course.setShortDescription("Resumo");
        course.setDescription("Descricao");
        course.setStatus(CourseStatus.PUBLISHED);
        course.setCategory("Hospedagem");
        course.setLevel("INTERMEDIARIO");
        course.setDurationTime(20);
        course.setDeadline(15);
        course.setPrice(50.0);
        course.setLearningObjectives(List.of("Aprender"));
        course.setRequireSequentialProgress(true);
        course.setEnforceDeadlineBlock(false);
        course.setMinQuizPassPercentage(70);
        course.setFeatured(false);
        course.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        course.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        course.setMajorVersion(1);
        course.setMinorVersion(0);
        return course;
    }

    private static User instructor(String name, String email) {
        final User instructor = new User(name, email, "hash", "ADMIN");
        instructor.setId(UUID.randomUUID());
        return instructor;
    }
}
