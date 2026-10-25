package com.escapa.backend.domain.entity;

import com.escapa.backend.domain.order.InvalidOrderTransitionException;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Fluxo APPROVED -> CANCELLED -> REFUNDED do pedido (US-28). */
class OrderTest {

    private static Order orderWith(OrderStatus status) {
        final Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        return order;
    }

    @Test
    void shouldCancelAnApprovedOrderAndRecordWhoAndWhy() {
        final Order order = orderWith(OrderStatus.APPROVED);
        final UUID adminId = UUID.randomUUID();

        order.cancel(adminId, "Pedido duplicado");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        final OrderStatusChange change = order.getStatusHistory().get(0);
        assertEquals(OrderStatus.CANCELLED, change.status());
        assertEquals(adminId, change.changedBy());
        assertEquals("Pedido duplicado", change.reason());
    }

    @Test
    void shouldRefundACancelledOrder() {
        final Order order = orderWith(OrderStatus.CANCELLED);

        order.refund(UUID.randomUUID(), null);

        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        assertNull(order.getStatusHistory().get(0).reason());
    }

    @Test
    void shouldOnlyCancelApprovedOrders() {
        assertThrows(InvalidOrderTransitionException.class,
                () -> orderWith(OrderStatus.CANCELLED).cancel(UUID.randomUUID(), null));
        assertThrows(InvalidOrderTransitionException.class,
                () -> orderWith(OrderStatus.REFUNDED).cancel(UUID.randomUUID(), null));
    }

    @Test
    void shouldOnlyRefundCancelledOrders() {
        assertThrows(InvalidOrderTransitionException.class,
                () -> orderWith(OrderStatus.APPROVED).refund(UUID.randomUUID(), null));
        assertThrows(InvalidOrderTransitionException.class,
                () -> orderWith(OrderStatus.REFUNDED).refund(UUID.randomUUID(), null));
    }

    @Test
    void shouldNotChangeStatusOrHistoryWhenTheTransitionIsRefused() {
        final Order order = orderWith(OrderStatus.APPROVED);

        assertThrows(InvalidOrderTransitionException.class, () -> order.refund(UUID.randomUUID(), null));

        assertEquals(OrderStatus.APPROVED, order.getStatus());
        assertEquals(0, order.getStatusHistory().size());
    }
}
