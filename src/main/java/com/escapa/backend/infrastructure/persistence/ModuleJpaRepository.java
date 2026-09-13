package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ModuleJpaRepository extends JpaRepository<ModuleEntity, UUID> {

    // Uma unica colecao "bag" (contents) no fetch, entao nao ha risco de
    // MultipleBagFetchException como no carregamento do curso completo.
    @EntityGraph(attributePaths = "contents")
    List<ModuleEntity> findByCourseIdOrderByOrderAsc(UUID courseId);

    @Query("SELECT MAX(m.order) FROM ModuleEntity m WHERE m.course.id = :courseId")
    Integer findMaxOrderByCourseId(@Param("courseId") UUID courseId);

    // flush/clear automaticos mantem as duas fases do reorder na ordem certa e
    // evitam que o contexto de persistencia devolva a ordem antiga depois.
    // O filtro por course_id garante que o reorder nunca alcance outro curso,
    // mesmo que um chamador futuro passe um id de fora da lista.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ModuleEntity m SET m.order = :order WHERE m.id = :id AND m.course.id = :courseId")
    void updateOrderByIdAndCourseId(
            @Param("id") UUID id,
            @Param("courseId") UUID courseId,
            @Param("order") Integer order
    );
}
