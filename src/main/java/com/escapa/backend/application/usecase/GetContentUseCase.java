package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.util.UUID;

public class GetContentUseCase {

    private final ContentRepositoryPort contentRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;

    public GetContentUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        this.contentRepositoryPort = contentRepositoryPort;
        this.moduleRepositoryPort = moduleRepositoryPort;
    }

    public Content execute(UUID moduleId, UUID id) {
        if (!moduleRepositoryPort.existsById(moduleId)) {
            throw new ModuleNotFoundException(moduleId);
        }
        final Content content = contentRepositoryPort.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));

        // Conteudo de outro modulo nao e visivel por esta rota.
        if (!moduleId.equals(content.getModuleId())) {
            throw new ContentNotFoundException(id);
        }
        return content;
    }
}
