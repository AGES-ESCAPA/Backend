package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.domain.course.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    /** Colunas da listagem administrativa, lidas sem carregar a entidade nem suas coleções. */
    interface ListingRow {
        UUID getId();

        String getTitle();

        String getCategory();

        Double getPrice();

        CourseStatus getStatus();

        Integer getMajorVersion();

        Integer getMinorVersion();
    }

    @Query("""
            SELECT c.id AS id, c.title AS title, c.category AS category, c.price AS price,
                   c.status AS status, c.majorVersion AS majorVersion, c.minorVersion AS minorVersion
            FROM CourseEntity c
            WHERE c.status <> :excludedStatus
            """)
    List<ListingRow> findListingRowsExcludingStatus(@Param("excludedStatus") CourseStatus excludedStatus);

    List<CourseEntity> findByTitleContainingIgnoreCaseAndIdNot(
            String title,
            UUID id
    );

    @Query("""
            SELECT DISTINCT c.category FROM CourseEntity c
            WHERE c.status = :status
            AND c.category IS NOT NULL
            AND c.category <> ''
            ORDER BY c.category
            """)
    List<String> findDistinctPublishedCategories(@Param("status") CourseStatus status);

    @Query("""
            SELECT DISTINCT c.category FROM CourseEntity c
            WHERE c.category IS NOT NULL
            AND c.category <> ''
            ORDER BY c.category
            """)
    List<String> findDistinctCategories();

    @Query("""
            SELECT DISTINCT c.level FROM CourseEntity c
            WHERE c.status = :status
            AND c.level IS NOT NULL
            AND c.level <> ''
            """)
    List<String> findDistinctPublishedLevels(@Param("status") CourseStatus status);

    @Query(value = """
            SELECT c FROM CourseEntity c
            LEFT JOIN FETCH c.instructor
            WHERE c.status = :status
            AND LOWER(c.title) LIKE :titlePattern
            AND (:category = '' OR c.category = :category)
            AND (:level = '' OR c.level = :level)
            """,
            countQuery = """
            SELECT COUNT(c) FROM CourseEntity c
            WHERE c.status = :status
            AND LOWER(c.title) LIKE :titlePattern
            AND (:category = '' OR c.category = :category)
            AND (:level = '' OR c.level = :level)
            """)
    Page<CourseEntity> findPublishedCourses(
            @Param("status") CourseStatus status,
            @Param("titlePattern") String titlePattern,
            @Param("category") String category,
            @Param("level") String level,
            Pageable pageable
    );

    /**
     * Carrega o curso com instrutor e modulos (uma unica colecao "bag" nesta
     * consulta). Materiais e conteudos dos modulos sao buscados em consultas
     * separadas (ver {@code course.CourseRepositoryAdapter}) para evitar o
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
