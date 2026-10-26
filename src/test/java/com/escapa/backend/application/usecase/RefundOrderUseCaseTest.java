package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.InvalidOrderTransitionException;
import com.escapa.backend.domain.order.OrderNotFoundException;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefundOrderUseCaseTest {

    private final InMemoryOrderRepositoryPort orders = new InMemoryOrderRepositoryPort();
    private final RefundOrderUseCase useCase = new RefundOrderUseCase(orders);
    private final UUID adminId = UUID.randomUUID();

    private Order givenOrder(OrderStatus status) {
        final Order order = new Order();
        order.setStatus(status);
        return orders.save(order);
    }

    @Test
    void shouldRefundACancelledOrderAndRecordTheHistory() {
        final Order order = givenOrder(OrderStatus.CANCELLED);

        final Order refunded = useCase.execute(order.getId(), adminId, "Estorno confirmado pelo banco");

        assertEquals(OrderStatus.REFUNDED, refunded.getStatus());
        assertEquals(OrderStatus.REFUNDED, orders.findById(order.getId()).orElseThrow().getStatus());
        final OrderStatusChange change = refunded.getStatusHistory().get(0);
        assertEquals(OrderStatus.REFUNDED, change.status());
        assertEquals(adminId, change.changedBy());
        assertEquals("Estorno confirmado pelo banco", change.reason());
    }

    @Test
    void shouldRefuseToRefundAnOrderThatIsNotCancelled() {
        final Order approved = givenOrder(OrderStatus.APPROVED);
        final Order refunded = givenOrder(OrderStatus.REFUNDED);

        assertThrows(InvalidOrderTransitionException.class, () -> useCase.execute(approved.getId(), adminId, null));
        assertThrows(InvalidOrderTransitionException.class, () -> useCase.execute(refunded.getId(), adminId, null));
        assertEquals(OrderStatus.APPROVED, orders.findById(approved.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldFailWhenTheOrderDoesNotExist() {
        assertThrows(OrderNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), adminId, null));
    }
}
