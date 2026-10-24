package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.InvalidOrderTransitionException;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderNotFoundException;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.SeatsInUseException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancelOrderUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);

    private final InMemoryOrderRepositoryPort orders = new InMemoryOrderRepositoryPort();
    private final FakeOrderAccessRevocationPort revocations = new FakeOrderAccessRevocationPort();
    private final FakeCompanySeatsPort seats = new FakeCompanySeatsPort();
    private final CancelOrderUseCase useCase = new CancelOrderUseCase(orders, revocations, seats, CLOCK);

    private final UUID adminId = UUID.randomUUID();
    private final UUID buyerId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private Order givenOrder(OrderType type, OrderItemType itemType, Integer seatsQuantity, OrderStatus status) {
        final Order order = new Order();
        order.setBuyerUserId(buyerId);
        order.setType(type);
        order.setItemType(itemType);
        order.setCompanyId(type == OrderType.CORPORATE ? companyId : null);
        order.setCourseId(itemType == OrderItemType.COURSE ? courseId : null);
        order.setSeatsQuantity(seatsQuantity);
        order.setStatus(status);
        return orders.save(order);
    }

    private Order stored(Order order) {
        return orders.findById(order.getId()).orElseThrow();
    }

    @Test
    void shouldCancelAnIndividualCourseOrderAndRevokeTheStudentAccess() {
        final Order order = givenOrder(OrderType.INDIVIDUAL, OrderItemType.COURSE, null, OrderStatus.APPROVED);

        final Order cancelled = useCase.execute(order.getId(), adminId, "Pedido do cliente");

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(OrderStatus.CANCELLED, stored(order).getStatus());
        final OrderStatusChange change = stored(order).getStatusHistory().get(0);
        assertEquals(adminId, change.changedBy());
        assertEquals("Pedido do cliente", change.reason());
        assertEquals(1, revocations.studentRevocations().size());
        assertEquals(buyerId, revocations.studentRevocations().get(0).ownerId());
        assertEquals(courseId, revocations.studentRevocations().get(0).courseId());
        assertEquals(CLOCK.instant().atZone(ZoneOffset.UTC).toLocalDateTime(),
                revocations.studentRevocations().get(0).revokedAt());
        assertTrue(revocations.companyRevocations().isEmpty());
    }

    @Test
    void shouldCancelACorporateCourseOrderAndRevokeTheCompanyAccess() {
        final Order order = givenOrder(OrderType.CORPORATE, OrderItemType.COURSE, null, OrderStatus.APPROVED);

        useCase.execute(order.getId(), adminId, null);

        assertEquals(OrderStatus.CANCELLED, stored(order).getStatus());
        assertEquals(1, revocations.companyRevocations().size());
        assertEquals(companyId, revocations.companyRevocations().get(0).ownerId());
        assertEquals(courseId, revocations.companyRevocations().get(0).courseId());
        assertTrue(revocations.studentRevocations().isEmpty());
    }

    @Test
    void shouldSubtractTheSeatsWhenThereAreEnoughFreeOnes() {
        seats.givenCompany(companyId, 10, 3);
        final Order order = givenOrder(OrderType.CORPORATE, OrderItemType.SEATS, 5, OrderStatus.APPROVED);

        useCase.execute(order.getId(), adminId, null);

        assertEquals(5, seats.findSeatsTotal(companyId));
        assertEquals(OrderStatus.CANCELLED, stored(order).getStatus());
        assertTrue(revocations.companyRevocations().isEmpty());
    }

    @Test
    void shouldAcceptWhenTheSeatsInUseMatchTheRemainingOnes() {
        seats.givenCompany(companyId, 10, 5);
        final Order order = givenOrder(OrderType.CORPORATE, OrderItemType.SEATS, 5, OrderStatus.APPROVED);

        useCase.execute(order.getId(), adminId, null);

        assertEquals(5, seats.findSeatsTotal(companyId));
    }

    @Test
    void shouldRefuseWhenCollaboratorsOccupyMoreSeatsThanWouldRemain() {
        seats.givenCompany(companyId, 10, 7);
        final Order order = givenOrder(OrderType.CORPORATE, OrderItemType.SEATS, 5, OrderStatus.APPROVED);
        final int savesBefore = orders.saveCount();

        final SeatsInUseException error = assertThrows(SeatsInUseException.class,
                () -> useCase.execute(order.getId(), adminId, null));

        assertEquals(7, error.getSeatsInUse());
        assertEquals(5, error.getSeatsAfterCancellation());
        assertEquals(2, error.getSeatsToRelease());
        assertTrue(error.getMessage().contains("7 seat(s) in use"));
        assertTrue(error.getMessage().contains("release 2 seat(s)"));
        assertEquals(10, seats.findSeatsTotal(companyId));
        assertEquals(OrderStatus.APPROVED, stored(order).getStatus());
        assertEquals(savesBefore, orders.saveCount());
    }

    @Test
    void shouldNeverLeaveANegativeSeatTotal() {
        seats.givenCompany(companyId, 3, 0);
        final Order order = givenOrder(OrderType.CORPORATE, OrderItemType.SEATS, 5, OrderStatus.APPROVED);

        useCase.execute(order.getId(), adminId, null);

        assertEquals(0, seats.findSeatsTotal(companyId));
    }

    @Test
    void shouldRefuseToCancelAnOrderThatIsNotApproved() {
        final Order cancelled = givenOrder(OrderType.INDIVIDUAL, OrderItemType.COURSE, null, OrderStatus.CANCELLED);
        final Order refunded = givenOrder(OrderType.INDIVIDUAL, OrderItemType.COURSE, null, OrderStatus.REFUNDED);

        assertThrows(InvalidOrderTransitionException.class, () -> useCase.execute(cancelled.getId(), adminId, null));
        assertThrows(InvalidOrderTransitionException.class, () -> useCase.execute(refunded.getId(), adminId, null));
        assertTrue(revocations.studentRevocations().isEmpty());
    }

    @Test
    void shouldFailWhenTheOrderDoesNotExist() {
        assertThrows(OrderNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), adminId, null));
    }

    @Test
    void shouldNotSaveTheNewStatusWhenTheRevocationFails() {
        final Order order = givenOrder(OrderType.INDIVIDUAL, OrderItemType.COURSE, null, OrderStatus.APPROVED);
        revocations.failWith(new IllegalStateException("database unavailable"));
        final int savesBefore = orders.saveCount();

        assertThrows(IllegalStateException.class, () -> useCase.execute(order.getId(), adminId, null));

        assertEquals(OrderStatus.APPROVED, stored(order).getStatus());
        assertEquals(0, stored(order).getStatusHistory().size());
        assertEquals(savesBefore, orders.saveCount());
    }
}
