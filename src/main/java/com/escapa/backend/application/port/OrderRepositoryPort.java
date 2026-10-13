package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de pedidos (US-26). O pedido é salvo e lido junto com o histórico
 * de mudanças de status.
 */
public interface OrderRepositoryPort {

    /** Cria ou atualiza o pedido; mudanças de status novas entram no histórico. */
    Order save(Order order);

    Optional<Order> findById(UUID id);

    /** Pedidos do comprador, do mais recente para o mais antigo. */
    List<Order> findByBuyerUserId(UUID buyerUserId);
}
