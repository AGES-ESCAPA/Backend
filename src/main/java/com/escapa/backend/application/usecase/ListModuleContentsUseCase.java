package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.util.List;
import java.util.UUID;

public class ListModuleContentsUseCase {

    private final ContentRepositoryPort contentRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;

    public ListModuleContentsUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        this.contentRepositoryPort = contentRepositoryPort;
        this.moduleRepositoryPort = moduleRepositoryPort;
    }

    public List<Content> execute(UUID moduleId) {
        if (moduleId == null) {
            throw new IllegalArgumentException("moduleId is required");
        }
        if (!moduleRepositoryPort.existsById(moduleId)) {
            throw new ModuleNotFoundException(moduleId);
        }
        return contentRepositoryPort.findByModuleId(moduleId);
    }
}
