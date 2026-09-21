package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.util.UUID;

/** Edita apenas o titulo; a posicao e alterada exclusivamente pelo reorder. */
public class UpdateModuleUseCase {

    private final ModuleRepositoryPort moduleRepositoryPort;

    public UpdateModuleUseCase(ModuleRepositoryPort moduleRepositoryPort) {
        this.moduleRepositoryPort = moduleRepositoryPort;
    }

    public Module execute(UUID id, String title) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        final Module module = moduleRepositoryPort.findById(id)
                .orElseThrow(() -> new ModuleNotFoundException(id));
        module.setTitle(title.trim());
        return moduleRepositoryPort.save(module);
    }
}
