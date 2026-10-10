package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "statusHistory")
    Optional<OrderEntity> findById(UUID id);

    @EntityGraph(attributePaths = "statusHistory")
    List<OrderEntity> findByBuyerUserIdOrderByCreatedAtDesc(UUID buyerUserId);
}
