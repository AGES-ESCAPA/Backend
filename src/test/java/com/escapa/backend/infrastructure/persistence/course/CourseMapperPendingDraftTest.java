package com.escapa.backend.infrastructure.persistence.course;

import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CourseMapperPendingDraftTest {

    @Test
    void shouldKeepThePublishedFieldsSeparateFromThePendingDraft() {
        final UUID instructorId = UUID.randomUUID();
        final UUID prerequisiteId = UUID.randomUUID();
        final Course course = new Course();
        course.setTitle("Publicado");
        course.setPrice(40.0);
        course.setPendingDraft(new CoursePendingDraft(
                "Rascunho", "resumo", "descricao", null, null, instructorId,
                "Hotelaria", "INICIANTE", 12, 30, 99.0, List.of("objetivo"),
                false, true, List.of(prerequisiteId)));

        final CourseEntity entity = new CourseEntity();
        CourseMapper.applyToEntity(course, entity);
        final Course restored = CourseMapper.toDomain(entity);

        assertEquals("Publicado", restored.getTitle());
        assertEquals(40.0, restored.getPrice());
        assertEquals("Rascunho", restored.getPendingDraft().title());
        assertEquals(99.0, restored.getPendingDraft().price());
        assertEquals(false, restored.getPendingDraft().requireSequentialProgress());
        assertEquals(List.of(prerequisiteId), restored.getPendingDraft().prerequisiteIds());
        assertNull(restored.getPendingDraft().thumbnailUrl());
    }
}
