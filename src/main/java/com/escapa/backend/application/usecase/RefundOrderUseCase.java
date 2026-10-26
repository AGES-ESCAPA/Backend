package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Registra o reembolso de um pedido já cancelado. Só muda o status e o histórico: o
 * acesso e os assentos já foram retirados no cancelamento.
 */
public class RefundOrderUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefundOrderUseCase.class);

    private final OrderRepositoryPort orderRepositoryPort;

    public RefundOrderUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public Order execute(UUID orderId, UUID adminId, String reason) {
        final Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.refund(adminId, reason);
        final Order saved = orderRepositoryPort.save(order);
        LOGGER.info("Order {} refunded by admin {}", saved.getId(), adminId);
        return saved;
    }
}
