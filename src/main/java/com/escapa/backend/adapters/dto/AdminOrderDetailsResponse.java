package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetailsResponse(
        UUID id,
        OrderType type,
        BuyerResponse buyer,
        OrderItemResponse item,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer installments,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderStatusChangeResponse> statusHistory,
        Integer collaboratorsWithAccessCount
) {

    public record BuyerResponse(
            UUID id,
            String name,
            String email,
            String document,
            String phone
    ) {
        static BuyerResponse from(OrderDetails.Buyer buyer) {
            if (buyer == null) {
                return null;
            }
            return new BuyerResponse(buyer.id(), buyer.name(), buyer.email(), buyer.document(), buyer.phone());
        }
    }

    public record OrderItemResponse(
            OrderItemType type,
            UUID courseId,
            String courseTitle,
            Integer seatsQuantity
    ) {
        static OrderItemResponse from(OrderDetails.Item item) {
            return new OrderItemResponse(item.type(), item.courseId(), item.courseTitle(), item.seatsQuantity());
        }
    }

    public record OrderStatusChangeResponse(
            UUID id,
            OrderStatus status,
            UUID changedBy,
            String reason,
            LocalDateTime createdAt
    ) {
        static OrderStatusChangeResponse from(OrderStatusChange change) {
            return new OrderStatusChangeResponse(
                    change.id(), change.status(), change.changedBy(), change.reason(), change.createdAt());
        }
    }

    public static AdminOrderDetailsResponse from(OrderDetails details) {
        return new AdminOrderDetailsResponse(
                details.id(),
                details.type(),
                BuyerResponse.from(details.buyer()),
                OrderItemResponse.from(details.item()),
                details.amount(),
                details.paymentMethod(),
                details.installments(),
                details.status(),
                details.createdAt(),
                details.statusHistory().stream().map(OrderStatusChangeResponse::from).toList(),
                details.collaboratorsWithAccessCount()
        );
    }
}
