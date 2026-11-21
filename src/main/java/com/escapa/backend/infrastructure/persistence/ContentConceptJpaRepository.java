package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.ContentConceptEntity;
import com.escapa.backend.infrastructure.persistence.entity.ContentConceptId;

public interface ContentConceptJpaRepository extends JpaRepository<ContentConceptEntity, ContentConceptId> {
    List<ContentConceptEntity> findByIdContentIdOrderByIdOrderAsc(UUID contentId);
}
