package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyEntity;
import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UsersCompanyJpaRepository extends JpaRepository<UsersCompanyEntity, UsersCompanyId> {
    boolean existsByIdUserId(UUID userId);
}

