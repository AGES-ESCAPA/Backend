package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContentJpaRepository extends JpaRepository<ContentEntity, UUID> {

    List<ContentEntity> findByModuleIdOrderByOrderAsc(UUID moduleId);

    @Query("SELECT MAX(c.order) FROM ContentEntity c WHERE c.module.id = :moduleId")
    Integer findMaxOrderByModuleId(@Param("moduleId") UUID moduleId);

    // flush/clear automaticos mantem as duas fases do reorder na ordem certa e
    // evitam que o contexto de persistencia devolva a ordem antiga depois.
    // O filtro por module_id garante que o reorder nunca alcance outro modulo,
    // mesmo que um chamador futuro passe um id de fora da lista.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ContentEntity c SET c.order = :order WHERE c.id = :id AND c.module.id = :moduleId")
    void updateOrderByIdAndModuleId(
            @Param("id") UUID id,
            @Param("moduleId") UUID moduleId,
            @Param("order") Integer order
    );
}
