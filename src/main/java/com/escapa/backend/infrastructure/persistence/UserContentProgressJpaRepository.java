package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserContentProgressJpaRepository
        extends JpaRepository<UserContentProgressEntity, UserContentProgressId> {

    // A tabela de progresso guarda so aluno e aula; o curso vem da subconsulta
    // content -> modules -> courses.
    @Query("SELECT p.id.contentId FROM UserContentProgressEntity p "
            + "WHERE p.id.userId = :userId "
            + "AND p.id.contentId IN ("
            + "SELECT c.id FROM ContentEntity c WHERE c.module.course.id = :courseId)")
    List<UUID> findCompletedContentIds(
            @Param("userId") UUID userId,
            @Param("courseId") UUID courseId
    );
}
