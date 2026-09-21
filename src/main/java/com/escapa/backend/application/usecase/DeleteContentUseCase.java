package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;

import java.util.UUID;

/**
 * Remocao definitiva. A troca por soft-delete quando ja houver progresso de aluno
 * segue como pendencia herdada da BE-04.
 */
public class DeleteContentUseCase {

    private final ContentRepositoryPort contentRepositoryPort;

    public DeleteContentUseCase(ContentRepositoryPort contentRepositoryPort) {
        this.contentRepositoryPort = contentRepositoryPort;
    }

    public void execute(UUID id) {
        if (contentRepositoryPort.findById(id).isEmpty()) {
            throw new ContentNotFoundException(id);
        }
        contentRepositoryPort.deleteById(id);
    }
}
