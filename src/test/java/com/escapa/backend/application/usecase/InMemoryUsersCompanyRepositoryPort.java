package com.escapa.backend.application.usecase;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.escapa.backend.application.port.UsersCompanyRepositoryPort;

final class InMemoryUsersCompanyRepositoryPort implements UsersCompanyRepositoryPort {
    private final Set<UUID> companyEmployeeUserIds = new HashSet<>();

    void linkToCompany(UUID userId) {
        companyEmployeeUserIds.add(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return companyEmployeeUserIds.contains(userId);
    }
}