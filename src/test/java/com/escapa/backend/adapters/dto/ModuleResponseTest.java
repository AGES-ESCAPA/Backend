package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleResponseTest {

    @Test
    void shouldMapContentSummariesAndAggregateDuration() {
        final UUID firstId = UUID.randomUUID();
        final Content video = content(firstId, "Vídeo", ContentType.VIDEO, 1, 12);
        final Content text = content(UUID.randomUUID(), "Texto", ContentType.TEXT, 2, null);
        final Module module = new Module(UUID.randomUUID(), UUID.randomUUID(), "Módulo", 3);
        module.setContents(List.of(video, text));

        final ModuleResponse response = ModuleResponse.from(module);

        assertEquals(2, response.totalContents());
        assertEquals(12, response.totalDurationMinutes());
        assertEquals(new ModuleResponse.ContentSummary(firstId, "Vídeo", ContentType.VIDEO, 1),
                response.contents().get(0));
        assertEquals("Texto", response.contents().get(1).title());
    }

    @Test
    void shouldReturnEmptyContentsAndZeroAggregatesWhenContentsAreAbsent() {
        final Module module = new Module(UUID.randomUUID(), UUID.randomUUID(), "Sem aulas", 1);
        module.setContents(null);

        final ModuleResponse response = ModuleResponse.from(module);

        assertEquals(List.of(), response.contents());
        assertEquals(0, response.totalContents());
        assertEquals(0, response.totalDurationMinutes());
    }

    private static Content content(UUID id, String title, ContentType type, int order, Integer duration) {
        final Content content = new Content();
        content.setId(id);
        content.setTitle(title);
        content.setType(type);
        content.setOrder(order);
        content.setDurationMinutes(duration);
        return content;
    }
}
