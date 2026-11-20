package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleMapperTest {

    @Test
    void shouldReturnNullForMissingEntity() {
        assertNull(ModuleMapper.toDomain(null));
    }

    @Test
    void shouldMapPublishedCourseAndPropagateItsStatusToContents() {
        final CourseEntity course = new CourseEntity();
        course.setId(UUID.randomUUID());
        course.setStatus(CourseStatus.PUBLISHED);
        final ModuleEntity entity = new ModuleEntity();
        entity.setId(UUID.randomUUID());
        entity.setCourse(course);
        entity.setTitle("Módulo publicado");
        entity.setOrder(2);
        final ContentEntity content = new ContentEntity();
        content.setId(UUID.randomUUID());
        content.setType(ContentType.VIDEO);
        entity.setContents(List.of(content));

        final Module module = ModuleMapper.toDomain(entity);

        assertEquals(course.getId(), module.getCourseId());
        assertTrue(module.isCoursePublished());
        assertEquals(1, module.getContents().size());
        assertTrue(module.getContents().get(0).isCoursePublished());
    }

    @Test
    void shouldMapModuleWithoutCourseOrContents() {
        final ModuleEntity entity = new ModuleEntity();
        entity.setContents(null);

        final Module module = ModuleMapper.toDomain(entity);

        assertNull(module.getCourseId());
        assertFalse(module.isCoursePublished());
    }
}
