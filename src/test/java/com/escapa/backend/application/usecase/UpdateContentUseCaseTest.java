package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateContentUseCaseTest {

    private final InMemoryContentRepositoryPort contentRepository = new InMemoryContentRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final CreateContentUseCase createUseCase = new CreateContentUseCase(contentRepository, moduleRepository);
    private final UpdateContentUseCase useCase = new UpdateContentUseCase(contentRepository);
    private final UUID moduleId = moduleRepository.createModule();

    private Content givenVideoContent() {
        return createUseCase.execute(moduleId, "Aula original", ContentType.VIDEO,
                "https://vimeo.com/1", 10, "Descricao", false);
    }

    @Test
    void shouldUpdateEditableFields() {
        final Content created = givenVideoContent();

        final Content updated = useCase.execute(created.getId(), " Aula revisada ", ContentType.VIDEO,
                "https://vimeo.com/2", 20, "Nova descricao", true);

        assertEquals("Aula revisada", updated.getTitle());
        assertEquals("https://vimeo.com/2", updated.getUrl());
        assertEquals(20, updated.getDurationMinutes());
        assertEquals("Nova descricao", updated.getDescription());
        assertTrue(updated.getIsFree());
    }

    @Test
    void shouldPreserveOrderAndModuleOnUpdate() {
        final Content created = givenVideoContent();

        final Content updated = useCase.execute(created.getId(), "Aula revisada", ContentType.VIDEO,
                "https://vimeo.com/2", 20, "Nova descricao", false);

        assertEquals(created.getOrder(), updated.getOrder());
        assertEquals(moduleId, updated.getModuleId());
    }

    @Test
    void shouldSwitchTypeAndClearFieldsOfThePreviousType() {
        final Content created = givenVideoContent();

        final Content updated = useCase.execute(created.getId(), "Agora e texto", ContentType.TEXT,
                null, null, "Conteudo em texto", false);

        assertEquals(ContentType.TEXT, updated.getType());
        assertNull(updated.getUrl());
        assertNull(updated.getDurationMinutes());
    }

    @Test
    void shouldApplyConditionalValidationWhenSwitchingType() {
        final Content created = givenVideoContent();

        final IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(created.getId(), "Agora e texto", ContentType.TEXT, null, null, null, false)
        );

        assertTrue(error.getMessage().contains("description"));
    }

    @Test
    void shouldRejectUnknownContent() {
        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), "Aula", ContentType.TEXT, null, null, "Texto", false)
        );
    }
}
