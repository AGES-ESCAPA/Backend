package com.escapa.backend.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;

public interface RegularUserJpaRepository extends JpaRepository<RegularUserEntity, UUID> {

    boolean existsByCpf(String cpf);
}
