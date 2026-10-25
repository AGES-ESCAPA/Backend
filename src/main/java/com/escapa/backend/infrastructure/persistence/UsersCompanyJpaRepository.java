package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyEntity;
import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UsersCompanyJpaRepository extends JpaRepository<UsersCompanyEntity, UsersCompanyId> {
    boolean existsByIdUserId(UUID userId);

    @Query("SELECT uc.id.companyId FROM UsersCompanyEntity uc WHERE uc.id.userId = :userId")
    List<UUID> findCompanyIdsByUserId(@Param("userId") UUID userId);
}

