package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.ModulePrerequisiteRepositoryPort;

@Repository
public class ModulePrerequisiteRepositoryAdapter implements ModulePrerequisiteRepositoryPort {

    private final ModulePrerequisiteJpaRepository modulePrerequisiteJpaRepository;

    public ModulePrerequisiteRepositoryAdapter(ModulePrerequisiteJpaRepository modulePrerequisiteJpaRepository) {
        this.modulePrerequisiteJpaRepository = modulePrerequisiteJpaRepository;
    }

    @Override
    public List<UUID> findPrerequisiteModuleIds(UUID moduleId) {
        return modulePrerequisiteJpaRepository.findPrerequisiteModuleIdsByModuleId(moduleId);
    }
}
