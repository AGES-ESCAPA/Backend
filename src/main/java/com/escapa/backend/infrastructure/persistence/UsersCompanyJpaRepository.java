package com.escapa.backend.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyEntity;
import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyId;

public interface UsersCompanyJpaRepository extends JpaRepository<UsersCompanyEntity, UsersCompanyId> {

    boolean existsByIdUserId(UUID userId);
}