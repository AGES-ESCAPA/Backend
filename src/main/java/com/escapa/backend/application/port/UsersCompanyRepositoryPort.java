package com.escapa.backend.application.port;

import java.util.Optional;
import java.util.UUID;

public interface UsersCompanyRepositoryPort {

    boolean existsByUserId(UUID userId);

    Optional<UUID> findCompanyIdByUserId(UUID userId);
}