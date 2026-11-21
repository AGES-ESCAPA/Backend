package com.escapa.backend.domain.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

class CoursePendingDraftTest {

    @Test
    void shouldCopyPublishedValuesAndPrerequisites() {
        final Course course = new Course();
        course.setTitle("Publicado");
        course.setLearningObjectives(null);

        final CoursePendingDraft emptyDraft = CoursePendingDraft.fromPublished(course, null);

        assertNull(emptyDraft.instructorId());
        assertTrue(emptyDraft.learningObjectives().isEmpty());
        assertTrue(emptyDraft.prerequisiteIds().isEmpty());

        final User instructor = new User("Instrutora", "instructor@example.com", "hash", "ADMIN");
        instructor.setId(UUID.randomUUID());
        course.setInstructor(instructor);
        course.setLearningObjectives(List.of("Objetivo"));
        final UUID prerequisiteId = UUID.randomUUID();

        final CoursePendingDraft populatedDraft = CoursePendingDraft.fromPublished(
                course, List.of(prerequisiteId));

        assertEquals(instructor.getId(), populatedDraft.instructorId());
        assertEquals(List.of("Objetivo"), populatedDraft.learningObjectives());
        assertEquals(List.of(prerequisiteId), populatedDraft.prerequisiteIds());
    }

    @Test
    void shouldPreserveNullUpdatesAndIgnoreBlankTitle() {
        final CoursePendingDraft draft = sampleDraft();

        final CoursePendingDraft unchanged = draft.applyUpdate(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        final CoursePendingDraft blankTitle = draft.applyUpdate(
                "   ", null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        assertEquals("Original", unchanged.title());
        assertEquals("Original", blankTitle.title());
        assertEquals("Resumo", unchanged.shortDescription());
        assertEquals("Descricao", unchanged.description());
        assertEquals(List.of("Objetivo"), unchanged.learningObjectives());
        assertEquals(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                unchanged.prerequisiteIds());
    }

    @Test
    void shouldApplyAndNormalizeProvidedUpdates() {
        final CoursePendingDraft updated = sampleDraft().applyUpdate(
                "  Novo titulo  ", "Novo resumo", "Nova descricao", "   ", " video ",
                UUID.fromString("00000000-0000-0000-0000-000000000002"), "Turismo", "AVANCADO",
                60, 90, 120.0, List.of("Novo objetivo"), false, true);

        assertEquals("Novo titulo", updated.title());
        assertEquals("Novo resumo", updated.shortDescription());
        assertEquals("Nova descricao", updated.description());
        assertNull(updated.thumbnailUrl());
        assertEquals("video", updated.teaserVideoUrl());
        assertEquals("Turismo", updated.category());
        assertEquals("AVANCADO", updated.level());
        assertEquals(60, updated.durationTime());
        assertEquals(90, updated.deadline());
        assertEquals(120.0, updated.price());
        assertEquals(List.of("Novo objetivo"), updated.learningObjectives());
        assertFalse(updated.requireSequentialProgress());
        assertTrue(updated.enforceDeadlineBlock());
    }

    @Test
    void shouldTreatBlankTeaserAsAbsent() {
        final CoursePendingDraft cleared = sampleDraft().applyUpdate(
                null, null, null, null, "   ", null, null, null,
                null, null, null, null, null, null);

        assertNull(cleared.teaserVideoUrl());
    }

    @Test
    void shouldManagePrerequisitesAndApplyNullableRules() {
        final UUID prerequisiteId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        final UUID anotherId = UUID.randomUUID();
        final CoursePendingDraft draft = sampleDraft();

        assertTrue(draft.containsPrerequisite(prerequisiteId));
        assertFalse(draft.containsPrerequisite(anotherId));
        assertTrue(draft.withPrerequisite(anotherId).containsPrerequisite(anotherId));
        assertFalse(draft.withoutPrerequisite(prerequisiteId).containsPrerequisite(prerequisiteId));
        assertFalse(new CoursePendingDraft(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null).containsPrerequisite(prerequisiteId));

        final Course course = new Course();
        course.setRequireSequentialProgress(true);
        course.setEnforceDeadlineBlock(false);
        new CoursePendingDraft(
                "Aplicado", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null).applyTo(course);

        assertEquals("Aplicado", course.getTitle());
        assertTrue(course.getLearningObjectives().isEmpty());
        assertTrue(course.getRequireSequentialProgress());
        assertFalse(course.getEnforceDeadlineBlock());
    }

    private static CoursePendingDraft sampleDraft() {
        return new CoursePendingDraft(
                "Original", "Resumo", "Descricao", "https://example.com/image.png", "video",
                UUID.fromString("00000000-0000-0000-0000-000000000003"), "Categoria", "INICIANTE",
                30, 45, 50.0, List.of("Objetivo"), true, false,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }
}
