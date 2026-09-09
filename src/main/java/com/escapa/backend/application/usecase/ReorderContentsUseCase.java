package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReorderContentsUseCase {

    private final ContentRepositoryPort contentRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;

    public ReorderContentsUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        this.contentRepositoryPort = contentRepositoryPort;
        this.moduleRepositoryPort = moduleRepositoryPort;
    }

    public List<Content> execute(UUID moduleId, List<UUID> orderedIds) {
        if (!moduleRepositoryPort.existsById(moduleId)) {
            throw new ModuleNotFoundException(moduleId);
        }
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("contentIds is required");
        }

        final Set<UUID> requested = new HashSet<>(orderedIds);
        if (requested.size() != orderedIds.size()) {
            throw new IllegalArgumentException("contentIds must not contain duplicates");
        }

        final Set<UUID> current = new HashSet<>(
                contentRepositoryPort.findByModuleId(moduleId).stream().map(Content::getId).toList());
        if (!current.equals(requested)) {
            // Reordenacao parcial deixaria buracos ou colisoes na sequencia do modulo.
            throw new IllegalArgumentException("contentIds must contain every content of the module exactly once");
        }

        contentRepositoryPort.reorder(moduleId, orderedIds);
        return contentRepositoryPort.findByModuleId(moduleId);
    }
}
