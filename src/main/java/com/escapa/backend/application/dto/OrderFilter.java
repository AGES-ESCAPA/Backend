package com.escapa.backend.application.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;

/**
 * Filtros opcionais para a listagem administrativa de pedidos; {@code null} não filtra.
 *
 * @param search trecho do código do pedido, do solicitante ou do título do curso; em branco não filtra
 */
public record OrderFilter(
        OrderType type,
        UUID courseId,
        LocalDate from,
        LocalDate to,
        OrderStatus status,
        String search
) {

    public OrderFilter {
        search = search == null || search.isBlank() ? null : search.strip();
    }

    public OrderFilter(OrderType type, UUID courseId, LocalDate from, LocalDate to, OrderStatus status) {
        this(type, courseId, from, to, status, null);
    }
}
