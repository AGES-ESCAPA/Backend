package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A registered purchase")
public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        OrderItemType itemType,
        UUID courseId,
        Integer seatsQuantity,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer installments,
        LocalDateTime createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItemType(),
                order.getCourseId(),
                order.getSeatsQuantity(),
                order.getAmount(),
                order.getPaymentMethod(),
                order.getInstallments(),
                order.getCreatedAt());
    }
}
