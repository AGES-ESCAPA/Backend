package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentMapperTest {

    @Test
    void shouldReturnNullWhenMappingNullEntity() {
        assertNull(ContentMapper.toDomain(null));
    }

    @Test
    void shouldMapContentAndOptionalModuleReference() {
        final UUID moduleId = UUID.randomUUID();
        final ModuleEntity module = new ModuleEntity();
        module.setId(moduleId);
        final LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        final ContentEntity entity = new ContentEntity();
        entity.setId(UUID.randomUUID());
        entity.setModule(module);
        entity.setTitle("Lesson");
        entity.setDescription("Description");
        entity.setType(ContentType.VIDEO);
        entity.setUrl("https://example.com/video");
        entity.setDurationMinutes(12);
        entity.setIsFree(true);
        entity.setOrder(3);
        entity.setRecursos("[{\"name\":\"slides\"}]");
        entity.setCreatedAt(createdAt);

        final Content mapped = ContentMapper.toDomain(entity);

        assertEquals(entity.getId(), mapped.getId());
        assertEquals(moduleId, mapped.getModuleId());
        assertEquals("Lesson", mapped.getTitle());
        assertEquals("Description", mapped.getDescription());
        assertEquals(ContentType.VIDEO, mapped.getType());
        assertEquals("https://example.com/video", mapped.getUrl());
        assertEquals(12, mapped.getDurationMinutes());
        assertTrue(mapped.getIsFree());
        assertEquals(3, mapped.getOrder());
        assertEquals(entity.getRecursos(), mapped.getResources());
        assertEquals(createdAt, mapped.getCreatedAt());

        entity.setModule(null);
        assertNull(ContentMapper.toDomain(entity).getModuleId());
    }

    @Test
    void shouldApplyFieldsAndInitializeCreationTimeOnlyWhenMissing() {
        final ModuleEntity module = new ModuleEntity();
        final Content content = new Content();
        content.setTitle("Lesson");
        content.setDescription("Description");
        content.setType(ContentType.VIDEO);
        content.setUrl("https://example.com/video");
        content.setDurationMinutes(12);
        content.setIsFree(null);
        content.setOrder(2);
        final ContentEntity entity = new ContentEntity();
        final LocalDateTime beforeMapping = LocalDateTime.now();

        ContentMapper.applyToEntity(content, entity, module);

        assertSame(module, entity.getModule());
        assertEquals("Lesson", entity.getTitle());
        assertEquals("Description", entity.getDescription());
        assertEquals(ContentType.VIDEO, entity.getType());
        assertEquals("https://example.com/video", entity.getUrl());
        assertEquals(12, entity.getDurationMinutes());
        assertFalse(entity.getIsFree());
        assertEquals(2, entity.getOrder());
        assertFalse(entity.getCreatedAt().isBefore(beforeMapping));

        final LocalDateTime preservedCreatedAt = LocalDateTime.of(2025, 2, 3, 4, 5);
        content.setIsFree(true);
        content.setCreatedAt(preservedCreatedAt);
        entity.setCreatedAt(preservedCreatedAt.minusDays(1));

        ContentMapper.applyToEntity(content, entity, module);

        assertTrue(entity.getIsFree());
        assertEquals(preservedCreatedAt.minusDays(1), entity.getCreatedAt());
    }
}
