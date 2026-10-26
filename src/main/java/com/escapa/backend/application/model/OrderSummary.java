package com.escapa.backend.application.model;

import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resumo de um pedido para a listagem da gestão de pedidos.
 *
 * @param requester nome do aluno ou razão social da empresa
 */
public record OrderSummary(
        UUID id,
        String requester,
        OrderType type,
        UUID courseId,
        String courseTitle,
        Integer seatsQuantity,
        LocalDateTime purchaseDate,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        OrderStatus status
) {
}
