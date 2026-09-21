package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateContentUseCaseTest {

    private final InMemoryContentRepositoryPort contentRepository = new InMemoryContentRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final CreateContentUseCase useCase = new CreateContentUseCase(contentRepository, moduleRepository);
    private final UUID moduleId = moduleRepository.createModule();

    @Test
    void shouldCreateVideoContentWithGeneratedOrder() {
        final Content content = useCase.execute(
                moduleId, " 1.3 Formularios ", ContentType.VIDEO, "https://vimeo.com/123456789",
                13, "Aula sobre formularios", false);

        assertNotNull(content.getId());
        assertEquals("1.3 Formularios", content.getTitle());
        assertEquals(moduleId, content.getModuleId());
        assertEquals(1, content.getOrder());
        assertNotNull(content.getCreatedAt());
        assertFalse(content.getIsFree());
    }

    @Test
    void shouldIncrementOrderFollowingTheLastContentOfTheModule() {
        useCase.execute(moduleId, "Aula 1", ContentType.TEXT, null, null, "Texto", false);
        useCase.execute(moduleId, "Aula 2", ContentType.TEXT, null, null, "Texto", false);
        final Content third = useCase.execute(moduleId, "Aula 3", ContentType.TEXT, null, null, "Texto", false);

        assertEquals(3, third.getOrder());
    }

    @Test
    void shouldKeepOrderSequenceIndependentPerModule() {
        final UUID otherModuleId = moduleRepository.createModule();
        useCase.execute(moduleId, "Aula 1", ContentType.TEXT, null, null, "Texto", false);

        final Content first = useCase.execute(otherModuleId, "Aula 1", ContentType.TEXT, null, null, "Texto", false);

        assertEquals(1, first.getOrder());
    }

    @Test
    void shouldRejectVideoWithoutUrl() {
        final IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, "Aula", ContentType.VIDEO, null, 13, "Descricao", false)
        );

        assertTrue(error.getMessage().contains("url"));
    }

    @Test
    void shouldRejectVideoWithoutDurationMinutes() {
        final IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, "Aula", ContentType.VIDEO, "https://vimeo.com/1", null,
                        "Descricao", false)
        );

        assertTrue(error.getMessage().contains("durationMinutes"));
    }

    @Test
    void shouldRejectFileWithoutUrl() {
        final IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, "Apostila", ContentType.FILE, "  ", null, "Descricao", false)
        );

        assertTrue(error.getMessage().contains("url"));
    }

    @Test
    void shouldRejectTextWithoutDescription() {
        final IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, "Resumo", ContentType.TEXT, null, null, null, false)
        );

        assertTrue(error.getMessage().contains("description"));
    }

    @Test
    void shouldRejectUnknownModule() {
        assertThrows(
                ModuleNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), "Aula", ContentType.TEXT, null, null, "Texto", false)
        );
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, "   ", ContentType.TEXT, null, null, "Texto", false)
        );
    }

    @Test
    void shouldTreatNullIsFreeAsPaidContent() {
        final Content content = useCase.execute(moduleId, "Aula", ContentType.TEXT, null, null, "Texto", null);

        assertFalse(content.getIsFree());
    }
}
