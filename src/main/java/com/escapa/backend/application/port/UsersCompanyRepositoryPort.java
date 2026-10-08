package com.escapa.backend.application.port;

import java.util.UUID;

public interface UsersCompanyRepositoryPort {

    boolean existsByUserId(UUID userId);
}