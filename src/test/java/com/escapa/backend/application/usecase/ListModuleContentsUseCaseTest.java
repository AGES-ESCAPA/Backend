package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListModuleContentsUseCaseTest {

    private final InMemoryContentRepositoryPort contentRepository = new InMemoryContentRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final CreateContentUseCase createUseCase = new CreateContentUseCase(contentRepository, moduleRepository);
    private final ListModuleContentsUseCase useCase =
            new ListModuleContentsUseCase(contentRepository, moduleRepository);
    private final UUID moduleId = moduleRepository.createModule();

    private Content givenContent(String title) {
        return createUseCase.execute(moduleId, title, ContentType.TEXT, null, null, "Texto", false);
    }

    @Test
    void shouldReturnContentsOrderedByOrder() {
        final Content first = givenContent("Aula 1");
        final Content second = givenContent("Aula 2");
        final Content third = givenContent("Aula 3");

        final List<Content> contents = useCase.execute(moduleId);

        assertEquals(List.of(first.getId(), second.getId(), third.getId()),
                contents.stream().map(Content::getId).toList());
        assertEquals(List.of(1, 2, 3), contents.stream().map(Content::getOrder).toList());
    }

    @Test
    void shouldReturnEmptyListForModuleWithoutContents() {
        assertTrue(useCase.execute(moduleId).isEmpty());
    }

    @Test
    void shouldNotLeakContentsFromAnotherModule() {
        givenContent("Aula 1");
        final UUID otherModuleId = moduleRepository.createModule();
        createUseCase.execute(otherModuleId, "Aula externa", ContentType.TEXT, null, null, "Texto", false);

        assertEquals(1, useCase.execute(moduleId).size());
    }

    @Test
    void shouldRejectUnknownModule() {
        assertThrows(ModuleNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
