package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.ContentReferenceEntity;

public interface ContentReferenceJpaRepository extends JpaRepository<ContentReferenceEntity, UUID> {
    List<ContentReferenceEntity> findByContentIdOrderByOrderAsc(UUID contentId);
}
