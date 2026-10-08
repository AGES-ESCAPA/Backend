package com.escapa.backend.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.UsersCompanyRepositoryPort;

@Repository
public class UsersCompanyRepositoryAdapter implements UsersCompanyRepositoryPort {

    private final UsersCompanyJpaRepository usersCompanyJpaRepository;

    public UsersCompanyRepositoryAdapter(UsersCompanyJpaRepository usersCompanyJpaRepository) {
        this.usersCompanyJpaRepository = usersCompanyJpaRepository;
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return usersCompanyJpaRepository.existsByIdUserId(userId);
    }
}