package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateModuleUseCaseTest {

    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final CreateModuleUseCase createUseCase = new CreateModuleUseCase(moduleRepository, courseRepository);
    private final UpdateModuleUseCase useCase = new UpdateModuleUseCase(moduleRepository);
    private final UUID courseId = courseRepository.save(new Course()).getId();

    @Test
    void shouldUpdateTitleAndKeepOrder() {
        createUseCase.execute(courseId, "Modulo 1");
        final Module second = createUseCase.execute(courseId, "Modulo 2");

        final Module updated = useCase.execute(second.getId(), "  Modulo 2: CSS  ");

        assertEquals("Modulo 2: CSS", updated.getTitle());
        assertEquals(2, updated.getOrder());
        assertEquals(courseId, updated.getCourseId());
        assertEquals("Modulo 2: CSS", moduleRepository.findById(second.getId()).orElseThrow().getTitle());
    }

    @Test
    void shouldRejectUnknownModule() {
        assertThrows(ModuleNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), "Titulo"));
    }

    @Test
    void shouldRejectBlankTitle() {
        final Module module = createUseCase.execute(courseId, "Modulo 1");

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(module.getId(), " "));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(module.getId(), null));
        assertEquals("Modulo 1", moduleRepository.findById(module.getId()).orElseThrow().getTitle());
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, "Titulo"));
    }
}
