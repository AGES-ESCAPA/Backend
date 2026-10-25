package com.escapa.backend.application.dto;

import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtros opcionais para a listagem administrativa de pedidos; {@code null} não filtra.
 */
public record OrderFilter(
        OrderType type,
        UUID courseId,
        LocalDate from,
        LocalDate to,
        OrderStatus status
) {
}
