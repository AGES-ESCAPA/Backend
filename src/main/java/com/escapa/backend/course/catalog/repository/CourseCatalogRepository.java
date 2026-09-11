package com.escapa.backend.course.catalog.repository;

import com.escapa.backend.course.catalog.dto.CourseCardResponse;
import com.escapa.backend.course.shared.entity.CourseEntity;
import com.escapa.backend.course.shared.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Leitura da vitrine pública. Estende {@link Repository} (marcador), não {@code JpaRepository},
 * de propósito: o catálogo nunca grava nem apaga curso. Quem escreve é
 * {@code course.management.repository.CourseRepository}.
 *
 * <p>Contrato dos parâmetros (o service garante que nenhum chega nulo, porque parâmetro
 * nulo em JPQL vira {@code bytea} no Postgres e quebra a consulta):
 * <ul>
 *   <li>{@code titlePattern}: padrão LIKE já em minúsculas, com {@code %} nas pontas e
 *       curingas do usuário escapados com {@code !}. Sem filtro, {@code "%"}.</li>
 *   <li>{@code category} e {@code level}: valor em minúsculas para igualdade, ou {@code ""} sem filtro.</li>
 * </ul>
 *
 * <p>Ordem fixa: mais recentes primeiro, desempate por id, para a paginação ser estável.
 */
public interface CourseCatalogRepository extends Repository<CourseEntity, UUID> {

    char LIKE_ESCAPE = '!';

    default Page<CourseCardResponse> findPublished(
            String titlePattern, String category, String level, Pageable pageable) {
        return findByStatus(CourseStatus.PUBLISHED, titlePattern, category, level, pageable);
    }

    @Query(value = """
            SELECT new com.escapa.backend.course.catalog.dto.CourseCardResponse(
                c.id, c.title, c.shortDescription, c.category, c.level, c.durationTime,
                c.lessonsCount, c.price, c.thumbnailUrl, i.name, c.ratingAverage, c.reviewsCount)
            FROM CourseEntity c
            LEFT JOIN c.instructor i
            WHERE c.status = :status
              AND LOWER(c.title) LIKE :titlePattern ESCAPE '!'
              AND (:category = '' OR LOWER(c.category) = :category)
              AND (:level = '' OR LOWER(c.level) = :level)
            ORDER BY c.createdAt DESC, c.id ASC
            """,
            countQuery = """
            SELECT COUNT(c)
            FROM CourseEntity c
            WHERE c.status = :status
              AND LOWER(c.title) LIKE :titlePattern ESCAPE '!'
              AND (:category = '' OR LOWER(c.category) = :category)
              AND (:level = '' OR LOWER(c.level) = :level)
            """)
    Page<CourseCardResponse> findByStatus(
            @Param("status") CourseStatus status,
            @Param("titlePattern") String titlePattern,
            @Param("category") String category,
            @Param("level") String level,
            Pageable pageable);
}
