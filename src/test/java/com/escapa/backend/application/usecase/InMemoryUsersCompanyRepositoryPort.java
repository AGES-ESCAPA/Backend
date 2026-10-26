package com.escapa.backend.application.usecase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.UsersCompanyRepositoryPort;

public final class InMemoryUsersCompanyRepositoryPort implements UsersCompanyRepositoryPort {
    private final Map<UUID, UUID> companyIdByEmployeeId = new HashMap<>();

    void linkToCompany(UUID userId) {
        linkToCompany(userId, UUID.randomUUID());
    }

    public void linkToCompany(UUID userId, UUID companyId) {
        companyIdByEmployeeId.put(userId, companyId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return companyIdByEmployeeId.containsKey(userId);
    }

    @Override
    public Optional<UUID> findCompanyIdByUserId(UUID userId) {
        return Optional.ofNullable(companyIdByEmployeeId.get(userId));
    }
}
