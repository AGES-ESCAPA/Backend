package com.escapa.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmail(String email);

    // Atualiza só o hash: o save() recria a entidade a partir do dominio e
    // sobrescreveria colunas que ele nao carrega, como o status.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.passwordHash = :passwordHash WHERE u.id = :id")
    int updatePasswordHash(@Param("id") UUID id, @Param("passwordHash") String passwordHash);
}
