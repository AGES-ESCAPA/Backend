package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.entity.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository repository;

    public OrderRepositoryAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        return OrderMapper.toDomain(repository.save(OrderMapper.toEntity(order)));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByBuyerUserId(UUID buyerUserId) {
        return repository.findByBuyerUserIdOrderByCreatedAtDesc(buyerUserId).stream()
                .map(OrderMapper::toDomain)
                .toList();
    }
}
