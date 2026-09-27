package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.ContentConceptEntity;
import com.escapa.backend.infrastructure.persistence.entity.ContentConceptId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentConceptJpaRepository extends JpaRepository<ContentConceptEntity, ContentConceptId> {
    List<ContentConceptEntity> findByIdContentIdOrderByIdOrderAsc(UUID contentId);
}
