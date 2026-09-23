package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.ModulePrerequisiteEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModulePrerequisiteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ModulePrerequisiteJpaRepository
        extends JpaRepository<ModulePrerequisiteEntity, ModulePrerequisiteId> {

    @Query("SELECT p.id.prerequisiteModuleId FROM ModulePrerequisiteEntity p WHERE p.id.moduleId = :moduleId")
    List<UUID> findPrerequisiteModuleIdsByModuleId(@Param("moduleId") UUID moduleId);
}
