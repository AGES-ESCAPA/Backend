package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;
import com.escapa.backend.infrastructure.persistence.entity.OrderEntity;
import com.escapa.backend.infrastructure.persistence.entity.OrderStatusHistoryEntity;

class OrderMapperTest {

    @Test
    void shouldMapDomainToEntityWithDefaultInstallmentsAndCreatedAt() {
        final Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setBuyerUserId(UUID.randomUUID());
        order.setType(OrderType.INDIVIDUAL);
        order.setItemType(OrderItemType.COURSE);
        order.setCourseId(UUID.randomUUID());
        order.setAmount(new BigDecimal("199.90"));
        order.setPaymentMethod(PaymentMethod.PIX);
        order.setInstallments(null);
        order.setStatus(OrderStatus.APPROVED);
        order.setCreatedAt(null);
        order.setStatusHistory(List.of(
                new OrderStatusChange(null, OrderStatus.APPROVED, order.getBuyerUserId(), "pago", null)));

        final OrderEntity entity = OrderMapper.toEntity(order);

        assertEquals(order.getId(), entity.getId());
        assertEquals(order.getBuyerUserId(), entity.getBuyerUserId());
        assertEquals(OrderType.INDIVIDUAL, entity.getType());
        assertEquals(OrderItemType.COURSE, entity.getItemType());
        assertEquals(order.getCourseId(), entity.getCourseId());
        assertEquals(new BigDecimal("199.90"), entity.getAmount());
        assertEquals(PaymentMethod.PIX, entity.getPaymentMethod());
        assertEquals(1, entity.getInstallments());
        assertEquals(OrderStatus.APPROVED, entity.getStatus());
        assertNotNull(entity.getCreatedAt());
        assertEquals(1, entity.getStatusHistory().size());
        assertEquals(OrderStatus.APPROVED, entity.getStatusHistory().get(0).getStatus());
        assertNotNull(entity.getStatusHistory().get(0).getCreatedAt());
    }

    @Test
    void shouldPreserveInstallmentsCreatedAtAndHistoryTimestamps() {
        final UUID historyId = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final LocalDateTime historyAt = createdAt.plusMinutes(5);
        final Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setBuyerUserId(UUID.randomUUID());
        order.setCompanyId(UUID.randomUUID());
        order.setType(OrderType.CORPORATE);
        order.setItemType(OrderItemType.SEATS);
        order.setSeatsQuantity(10);
        order.setAmount(new BigDecimal("500.00"));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setInstallments(3);
        order.setStatus(OrderStatus.APPROVED);
        order.setCreatedAt(createdAt);
        order.setStatusHistory(List.of(
                new OrderStatusChange(historyId, OrderStatus.APPROVED, order.getBuyerUserId(), null, historyAt)));

        final OrderEntity entity = OrderMapper.toEntity(order);

        assertEquals(3, entity.getInstallments());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(10, entity.getSeatsQuantity());
        assertEquals(order.getCompanyId(), entity.getCompanyId());
        assertEquals(historyId, entity.getStatusHistory().get(0).getId());
        assertEquals(historyAt, entity.getStatusHistory().get(0).getCreatedAt());
        assertNull(entity.getStatusHistory().get(0).getReason());
    }

    @Test
    void shouldMapEntityToDomainIncludingHistory() {
        final UUID orderId = UUID.randomUUID();
        final UUID buyerId = UUID.randomUUID();
        final UUID historyId = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setBuyerUserId(buyerId);
        entity.setType(OrderType.INDIVIDUAL);
        entity.setItemType(OrderItemType.COURSE);
        entity.setCourseId(UUID.randomUUID());
        entity.setAmount(new BigDecimal("99.00"));
        entity.setPaymentMethod(PaymentMethod.PIX);
        entity.setInstallments(1);
        entity.setStatus(OrderStatus.CANCELLED);
        entity.setCreatedAt(createdAt);

        final OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setId(historyId);
        history.setOrder(entity);
        history.setStatus(OrderStatus.CANCELLED);
        history.setChangedBy(buyerId);
        history.setReason("cancelado");
        history.setCreatedAt(createdAt.plusHours(1));
        entity.getStatusHistory().add(history);

        final Order domain = OrderMapper.toDomain(entity);

        assertEquals(orderId, domain.getId());
        assertEquals(buyerId, domain.getBuyerUserId());
        assertEquals(OrderStatus.CANCELLED, domain.getStatus());
        assertEquals(1, domain.getStatusHistory().size());
        assertEquals(historyId, domain.getStatusHistory().get(0).id());
        assertEquals("cancelado", domain.getStatusHistory().get(0).reason());
        assertEquals(createdAt.plusHours(1), domain.getStatusHistory().get(0).createdAt());
    }
}
