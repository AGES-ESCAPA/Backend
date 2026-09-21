package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReorderContentsUseCaseTest {

    private final InMemoryContentRepositoryPort contentRepository = new InMemoryContentRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final CreateContentUseCase createUseCase = new CreateContentUseCase(contentRepository, moduleRepository);
    private final ReorderContentsUseCase useCase = new ReorderContentsUseCase(contentRepository, moduleRepository);
    private final UUID moduleId = moduleRepository.createModule();

    private Content givenContent(String title) {
        return createUseCase.execute(moduleId, title, ContentType.TEXT, null, null, "Texto", false);
    }

    @Test
    void shouldRewriteOrderFollowingTheRequestedSequence() {
        final Content first = givenContent("Aula 1");
        final Content second = givenContent("Aula 2");
        final Content third = givenContent("Aula 3");

        final List<Content> reordered = useCase.execute(
                moduleId, List.of(third.getId(), first.getId(), second.getId()));

        assertEquals(List.of(third.getId(), first.getId(), second.getId()),
                reordered.stream().map(Content::getId).toList());
        assertEquals(List.of(1, 2, 3), reordered.stream().map(Content::getOrder).toList());
    }

    @Test
    void shouldRejectPartialSequence() {
        final Content first = givenContent("Aula 1");
        givenContent("Aula 2");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, List.of(first.getId()))
        );
    }

    @Test
    void shouldRejectDuplicatedIds() {
        final Content first = givenContent("Aula 1");
        givenContent("Aula 2");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, List.of(first.getId(), first.getId()))
        );
    }

    @Test
    void shouldRejectContentFromAnotherModule() {
        final Content first = givenContent("Aula 1");
        final UUID otherModuleId = moduleRepository.createModule();
        final Content foreign = createUseCase.execute(
                otherModuleId, "Aula externa", ContentType.TEXT, null, null, "Texto", false);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, List.of(first.getId(), foreign.getId()))
        );
    }

    @Test
    void shouldRejectEmptyList() {
        givenContent("Aula 1");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(moduleId, List.of())
        );
    }

    @Test
    void shouldRejectUnknownModule() {
        assertThrows(
                ModuleNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), List.of(UUID.randomUUID()))
        );
    }
}
