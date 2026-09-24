package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.ContentReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentReferenceJpaRepository extends JpaRepository<ContentReferenceEntity, UUID> {
    List<ContentReferenceEntity> findByContentIdOrderByOrderAsc(UUID contentId);
}
