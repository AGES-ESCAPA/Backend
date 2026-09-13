package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteModuleUseCaseTest {

    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final CreateModuleUseCase createUseCase = new CreateModuleUseCase(moduleRepository, courseRepository);
    private final DeleteModuleUseCase useCase = new DeleteModuleUseCase(moduleRepository);
    private final UUID courseId = courseRepository.save(new Course()).getId();

    @Test
    void shouldRemoveModule() {
        final Module module = createUseCase.execute(courseId, "Modulo 1");

        useCase.execute(module.getId());

        assertFalse(moduleRepository.existsById(module.getId()));
    }

    @Test
    void shouldKeepOtherModulesAndAppendAfterTheRemainingMax() {
        final Module first = createUseCase.execute(courseId, "Modulo 1");
        final Module second = createUseCase.execute(courseId, "Modulo 2");

        useCase.execute(first.getId());
        final Module third = createUseCase.execute(courseId, "Modulo 3");

        // Sem compactacao: o proximo modulo entra depois do maior order restante.
        assertEquals(2, moduleRepository.findById(second.getId()).orElseThrow().getOrder());
        assertEquals(3, third.getOrder());
    }

    @Test
    void shouldRejectUnknownModule() {
        assertThrows(ModuleNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
