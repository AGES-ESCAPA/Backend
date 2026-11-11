package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCurriculum;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.util.UUID;

/**
 * Em curso publicado, o módulo some do construtor e continua visível para o
 * aluno até a publicação. Fora isso, a remoção é definitiva.
 */
public class DeleteModuleUseCase {

    private final ModuleRepositoryPort moduleRepositoryPort;

    public DeleteModuleUseCase(ModuleRepositoryPort moduleRepositoryPort) {
        this.moduleRepositoryPort = moduleRepositoryPort;
    }

    public void execute(UUID id) {
        final Module module = moduleRepositoryPort.findById(id)
                .orElseThrow(() -> new ModuleNotFoundException(id));
        if (!module.isHeldForPublish()) {
            moduleRepositoryPort.deleteById(id);
            return;
        }
        module.setPendingDeletion(true);
        module.setOrder(PublishedCurriculum.parkedOrder(module.getOrder()));
        moduleRepositoryPort.save(module);
    }
}
