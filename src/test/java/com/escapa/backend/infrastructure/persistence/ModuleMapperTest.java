package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModuleMapperTest {

    @Test
    void shouldMapNullAndOptionalRelations() {
        assertNull(ModuleMapper.toDomain(null));

        final ModuleEntity entity = new ModuleEntity();
        entity.setTitle("Module");
        entity.setOrder(2);
        entity.setContents(null);

        final Module mappedWithoutRelations = ModuleMapper.toDomain(entity);

        assertNull(mappedWithoutRelations.getCourseId());
        assertEquals("Module", mappedWithoutRelations.getTitle());
        assertEquals(2, mappedWithoutRelations.getOrder());
        assertTrue(mappedWithoutRelations.getContents().isEmpty());
    }

    @Test
    void shouldMapCourseIdAndContentsWithoutMappingTheCourseAggregate() {
        final UUID courseId = UUID.randomUUID();
        final CourseEntity course = new CourseEntity();
        course.setId(courseId);
        final ContentEntity content = new ContentEntity();
        content.setTitle("Lesson");
        final ModuleEntity entity = new ModuleEntity();
        entity.setCourse(course);
        entity.setContents(List.of(content));

        final Module mapped = ModuleMapper.toDomain(entity);

        assertEquals(courseId, mapped.getCourseId());
        assertEquals(1, mapped.getContents().size());
        assertEquals("Lesson", mapped.getContents().get(0).getTitle());
    }

    @Test
    void shouldApplyEditableFieldsToEntity() {
        final Module module = new Module();
        module.setTitle("Updated");
        module.setOrder(4);
        final ModuleEntity entity = new ModuleEntity();
        final CourseEntity course = new CourseEntity();

        ModuleMapper.applyToEntity(module, entity, course);

        assertSame(course, entity.getCourse());
        assertEquals("Updated", entity.getTitle());
        assertEquals(4, entity.getOrder());
    }
}
