package com.escapa.backend.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.content.ContentType;

class ModuleTest {

    @Test
    void shouldCountContentsAndSumDurationsIgnoringNulls() {
        final Module module = new Module(UUID.randomUUID(), UUID.randomUUID(), "Modulo", 1);
        final Content withDuration = new Content();
        withDuration.setDurationMinutes(10);
        withDuration.setType(ContentType.VIDEO);
        final Content withoutDuration = new Content();
        withoutDuration.setDurationMinutes(null);
        withoutDuration.setType(ContentType.TEXT);
        module.getContents().add(withDuration);
        module.getContents().add(withoutDuration);

        assertEquals(2, module.getTotalContents());
        assertEquals(10, module.getTotalDurationMinutes());
    }

    @Test
    void shouldTreatNullContentsAsEmptyAggregates() {
        final Module module = new Module(UUID.randomUUID(), UUID.randomUUID(), "Modulo", 1);
        module.setContents(null);

        assertEquals(0, module.getTotalContents());
        assertEquals(0, module.getTotalDurationMinutes());
    }

    @Test
    void shouldDetectHeldForPublishOnlyWhenCourseIsPublishedWithOrder() {
        final Module module = new Module(UUID.randomUUID(), UUID.randomUUID(), "Modulo", 1);

        assertFalse(module.isHeldForPublish());
        module.setCoursePublished(true);
        assertFalse(module.isHeldForPublish());
        module.setPublishedOrder(1);
        assertTrue(module.isHeldForPublish());
    }

    @Test
    void shouldCopyCourseIdFromCourseAggregateConstructor() {
        final Course course = new Course();
        course.setId(UUID.randomUUID());
        final Module module = new Module(UUID.randomUUID(), course, "Modulo", 2);

        assertEquals(course.getId(), module.getCourseId());
        assertEquals(course, module.getCourse());
        assertTrue(module.getContents() instanceof ArrayList);
    }
}
