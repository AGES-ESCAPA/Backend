package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetContentUseCaseTest {

    private final InMemoryContentRepositoryPort contentRepository = new InMemoryContentRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final CreateContentUseCase createUseCase = new CreateContentUseCase(contentRepository, moduleRepository);
    private final GetContentUseCase useCase = new GetContentUseCase(contentRepository, moduleRepository);
    private final UUID moduleId = moduleRepository.createModule();

    @Test
    void shouldReturnContentOfTheModule() {
        final Content created = createUseCase.execute(
                moduleId, "Aula", ContentType.TEXT, null, null, "Texto", false);

        final Content found = useCase.execute(moduleId, created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Aula", found.getTitle());
    }

    @Test
    void shouldRejectContentBelongingToAnotherModule() {
        final UUID otherModuleId = moduleRepository.createModule();
        final Content foreign = createUseCase.execute(
                otherModuleId, "Aula externa", ContentType.TEXT, null, null, "Texto", false);

        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(moduleId, foreign.getId())
        );
    }

    @Test
    void shouldRejectUnknownContent() {
        assertThrows(
                ContentNotFoundException.class,
                () -> useCase.execute(moduleId, UUID.randomUUID())
        );
    }

    @Test
    void shouldRejectUnknownModule() {
        assertThrows(
                ModuleNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), UUID.randomUUID())
        );
    }
}
