package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "statusHistory")
    Optional<OrderEntity> findById(UUID id);

    @EntityGraph(attributePaths = "statusHistory")
    List<OrderEntity> findByBuyerUserIdOrderByCreatedAtDesc(UUID buyerUserId);

    /**
     * Trava a linha do pedido ate o fim da transacao. Sem o EntityGraph de proposito:
     * o Postgres nao aceita FOR UPDATE sobre o lado nulavel de um outer join.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> lockById(@Param("id") UUID id);
}
