package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void shouldKeepPublishedContentUntilTheCourseIsRepublished() {
        final Content created = createUseCase.execute(
                moduleId, "Aula publicada", ContentType.TEXT, null, null, "Texto", false);
        created.setCoursePublished(true);
        created.setPublishedOrder(2);
        created.setOrder(5);
        contentRepository.save(created);

        useCase.execute(created.getId());

        final Content saved = contentRepository.findById(created.getId()).orElseThrow();
        assertTrue(saved.isPendingDeletion());
        assertEquals(-1_000_005, saved.getOrder());
    }
}
