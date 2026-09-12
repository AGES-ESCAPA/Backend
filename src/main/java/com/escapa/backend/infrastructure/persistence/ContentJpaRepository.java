package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentJpaRepository extends JpaRepository<ContentEntity, UUID> {

    List<ContentEntity> findByModule_IdInOrderByOrderAsc(List<UUID> moduleIds);
}
