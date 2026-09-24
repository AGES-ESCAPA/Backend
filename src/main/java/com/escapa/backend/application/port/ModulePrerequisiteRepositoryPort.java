package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

public interface ModulePrerequisiteRepositoryPort {

    /** Ids dos modulos que sao pre-requisito do modulo informado. */
    List<UUID> findPrerequisiteModuleIds(UUID moduleId);
}
