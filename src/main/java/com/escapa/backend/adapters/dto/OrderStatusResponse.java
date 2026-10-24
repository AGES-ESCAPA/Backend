package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Pedido depois de uma ação de status: o novo status e o histórico em ordem cronológica. */
public record OrderStatusResponse(UUID id, OrderStatus status, List<StatusChange> statusHistory) {

    public static OrderStatusResponse from(Order order) {
        return new OrderStatusResponse(
                order.getId(),
                order.getStatus(),
                order.getStatusHistory().stream().map(StatusChange::from).toList()
        );
    }

    public record StatusChange(OrderStatus status, UUID changedBy, String reason, LocalDateTime createdAt) {

        static StatusChange from(OrderStatusChange change) {
            return new StatusChange(change.status(), change.changedBy(), change.reason(), change.createdAt());
        }
    }
}
