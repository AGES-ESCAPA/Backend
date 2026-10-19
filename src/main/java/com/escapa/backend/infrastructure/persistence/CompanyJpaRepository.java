package com.escapa.backend.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.escapa.backend.infrastructure.persistence.entity.CompanyEntity;

@Repository
public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, UUID> {
    boolean existsByCnpjId(String cnpjId);
}