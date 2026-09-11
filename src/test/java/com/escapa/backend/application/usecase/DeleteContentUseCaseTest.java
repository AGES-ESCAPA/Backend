package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteContentUseCaseTest {

    private final InMemoryContentRepositoryPort contentRepository = new InMemoryContentRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final CreateContentUseCase createUseCase = new CreateContentUseCase(contentRepository, moduleRepository);
    private final DeleteContentUseCase useCase = new DeleteContentUseCase(contentRepository);
    private final UUID moduleId = moduleRepository.createModule();

    @Test
    void shouldRemoveContent() {
        final Content created = createUseCase.execute(
                moduleId, "Aula", ContentType.TEXT, null, null, "Texto", false);

        useCase.execute(created.getId());

        assertTrue(contentRepository.findById(created.getId()).isEmpty());
    }

    @Test
    void shouldRejectUnknownContent() {
        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID())
        );
    }
}
