package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.util.UUID;

/**
 * Remocao definitiva do modulo e, por cascade, dos seus conteudos.
 * A troca por soft-delete quando ja houver progresso de aluno sobre o conteudo
 * fica registrada como pendencia da US-06 (fora do escopo desta task).
 */
public class DeleteModuleUseCase {

    private final ModuleRepositoryPort moduleRepositoryPort;

    public DeleteModuleUseCase(ModuleRepositoryPort moduleRepositoryPort) {
        this.moduleRepositoryPort = moduleRepositoryPort;
    }

    public void execute(UUID id) {
        if (!moduleRepositoryPort.existsById(id)) {
            throw new ModuleNotFoundException(id);
        }
        moduleRepositoryPort.deleteById(id);
    }
}
