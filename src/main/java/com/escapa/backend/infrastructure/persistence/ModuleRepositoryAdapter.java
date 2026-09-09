package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.ModuleRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ModuleRepositoryAdapter implements ModuleRepositoryPort {
    private final ModuleJpaRepository moduleJpaRepository;

    public ModuleRepositoryAdapter(ModuleJpaRepository moduleJpaRepository) {
        this.moduleJpaRepository = moduleJpaRepository;
    }

    @Override
    public boolean existsById(UUID id) {
        return id != null && moduleJpaRepository.existsById(id);
    }
}
