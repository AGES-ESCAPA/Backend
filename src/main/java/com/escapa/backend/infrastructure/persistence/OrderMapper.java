package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.infrastructure.persistence.entity.OrderEntity;
import com.escapa.backend.infrastructure.persistence.entity.OrderStatusHistoryEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;

final class OrderMapper {

    private OrderMapper() {
    }

    static OrderEntity toEntity(Order order) {
        final LocalDateTime now = LocalDateTime.now();
        final OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setBuyerUserId(order.getBuyerUserId());
        entity.setCompanyId(order.getCompanyId());
        entity.setType(order.getType());
        entity.setItemType(order.getItemType());
        entity.setCourseId(order.getCourseId());
        entity.setSeatsQuantity(order.getSeatsQuantity());
        entity.setAmount(order.getAmount());
        entity.setPaymentMethod(order.getPaymentMethod());
        entity.setInstallments(order.getInstallments() == null ? 1 : order.getInstallments());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt() == null ? now : order.getCreatedAt());
        for (final OrderStatusChange change : order.getStatusHistory()) {
            entity.getStatusHistory().add(toHistoryEntity(entity, change, now));
        }
        return entity;
    }

    static Order toDomain(OrderEntity entity) {
        final Order order = new Order();
        order.setId(entity.getId());
        order.setBuyerUserId(entity.getBuyerUserId());
        order.setCompanyId(entity.getCompanyId());
        order.setType(entity.getType());
        order.setItemType(entity.getItemType());
        order.setCourseId(entity.getCourseId());
        order.setSeatsQuantity(entity.getSeatsQuantity());
        order.setAmount(entity.getAmount());
        order.setPaymentMethod(entity.getPaymentMethod());
        order.setInstallments(entity.getInstallments());
        order.setStatus(entity.getStatus());
        order.setCreatedAt(entity.getCreatedAt());
        order.setStatusHistory(new ArrayList<>(entity.getStatusHistory().stream()
                .map(OrderMapper::toChange)
                .toList()));
        return order;
    }

    private static OrderStatusHistoryEntity toHistoryEntity(
            OrderEntity order, OrderStatusChange change, LocalDateTime now) {
        final OrderStatusHistoryEntity entity = new OrderStatusHistoryEntity();
        entity.setId(change.id());
        entity.setOrder(order);
        entity.setStatus(change.status());
        entity.setChangedBy(change.changedBy());
        entity.setReason(change.reason());
        entity.setCreatedAt(change.createdAt() == null ? now : change.createdAt());
        return entity;
    }

    private static OrderStatusChange toChange(OrderStatusHistoryEntity entity) {
        return new OrderStatusChange(
                entity.getId(), entity.getStatus(), entity.getChangedBy(), entity.getReason(), entity.getCreatedAt());
    }
}
