package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.ModulePrerequisiteRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

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
