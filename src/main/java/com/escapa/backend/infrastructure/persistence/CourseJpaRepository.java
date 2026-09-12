package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    /**
     * Carrega o curso com instrutor e modulos (uma unica colecao "bag" nesta
     * consulta). Materiais e conteudos dos modulos sao buscados em consultas
     * separadas (ver {@link CourseRepositoryAdapter}) para evitar o
     * {@code org.hibernate.loader.MultipleBagFetchException}, lancado quando
     * mais de uma colecao List sem indice ("bag") e buscada via fetch join
     * na mesma consulta.
     */
    @EntityGraph(attributePaths = {
            "instructor",
            "modules"
    })
    Optional<CourseEntity> findWithModulesById(UUID id);

    @EntityGraph(attributePaths = {
            "materials"
    })
    Optional<CourseEntity> findWithMaterialsById(UUID id);
}
