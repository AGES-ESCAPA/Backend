package com.escapa.backend.application.model;

import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detalhes consolidados de um pedido para a tela de visualização do administrador.
 *
 * @param collaboratorsWithAccessCount só em pedido corporativo de curso; {@code null} nos demais
 */
public record OrderDetails(
        UUID id,
        OrderType type,
        Buyer buyer,
        Item item,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer installments,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderStatusChange> statusHistory,
        Integer collaboratorsWithAccessCount
) {

    /** @param document CPF do aluno ou CNPJ da empresa */
    public record Buyer(
            UUID id,
            String name,
            String email,
            String document,
            String phone
    ) {
    }

    public record Item(
            OrderItemType type,
            UUID courseId,
            String courseTitle,
            Integer seatsQuantity
    ) {
    }
}
