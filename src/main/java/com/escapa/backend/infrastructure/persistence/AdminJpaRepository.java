package com.escapa.backend.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;

public interface AdminJpaRepository extends JpaRepository<AdminEntity, UUID> {
}
