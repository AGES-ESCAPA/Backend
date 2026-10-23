package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderListItemResponse(
        UUID id,
        String requester,
        OrderType type,
        String courseTitle,
        Integer seatsQuantity,
        LocalDateTime purchaseDate,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        OrderStatus status
) {

    public static AdminOrderListItemResponse from(OrderSummary summary) {
        return new AdminOrderListItemResponse(
                summary.id(),
                summary.requester(),
                summary.type(),
                summary.courseTitle(),
                summary.seatsQuantity(),
                summary.purchaseDate(),
                summary.paymentMethod(),
                summary.amount(),
                summary.status()
        );
    }
}
