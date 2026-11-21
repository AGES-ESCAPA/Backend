package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.OrderStats;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;

class GetAdminOrderStatsUseCaseTest {

    private InMemoryOrderRepositoryPort repository;
    private GetAdminOrderStatsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepositoryPort();
        useCase = new GetAdminOrderStatsUseCase(repository);
    }

    private void givenOrder(OrderType type, OrderStatus status, String amount, LocalDateTime purchaseDate) {
        repository.addSummary(new OrderSummary(
                UUID.randomUUID(), "Comprador", type, UUID.randomUUID(), "Curso", null,
                purchaseDate, PaymentMethod.CARD, new BigDecimal(amount), status));
    }

    @Test
    void shouldReturnZeroTotalsWhenNoOrdersExist() {
        final OrderStats stats = useCase.execute(null, null, null, null, null, null);

        assertEquals(0, stats.approvedCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.approvedRevenue()));
        assertEquals(0, stats.cancelledOrRefundedCount());
    }

    @Test
    void shouldSumTheApprovedOrdersAndCountTheOthers() {
        final LocalDateTime now = LocalDateTime.now();
        givenOrder(OrderType.INDIVIDUAL, OrderStatus.APPROVED, "100.00", now);
        givenOrder(OrderType.CORPORATE, OrderStatus.APPROVED, "250.00", now);
        givenOrder(OrderType.INDIVIDUAL, OrderStatus.CANCELLED, "80.00", now);
        givenOrder(OrderType.INDIVIDUAL, OrderStatus.REFUNDED, "60.00", now);

        final OrderStats stats = useCase.execute(null, null, null, null, null, null);

        assertEquals(2, stats.approvedCount());
        assertEquals(0, new BigDecimal("350.00").compareTo(stats.approvedRevenue()));
        assertEquals(2, stats.cancelledOrRefundedCount());
    }

    @Test
    void shouldApplyTheSameFiltersAsTheListing() {
        givenOrder(OrderType.INDIVIDUAL, OrderStatus.APPROVED, "100.00", LocalDateTime.of(2026, 3, 10, 10, 0));
        givenOrder(OrderType.INDIVIDUAL, OrderStatus.REFUNDED, "80.00", LocalDateTime.of(2026, 3, 12, 10, 0));
        givenOrder(OrderType.CORPORATE, OrderStatus.APPROVED, "250.00", LocalDateTime.of(2026, 3, 10, 10, 0));
        givenOrder(OrderType.INDIVIDUAL, OrderStatus.APPROVED, "40.00", LocalDateTime.of(2026, 4, 2, 10, 0));

        final OrderStats stats = useCase.execute(
                OrderType.INDIVIDUAL, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null);

        assertEquals(1, stats.approvedCount());
        assertEquals(0, new BigDecimal("100.00").compareTo(stats.approvedRevenue()));
        assertEquals(1, stats.cancelledOrRefundedCount());
    }

    @Test
    void shouldThrowExceptionWhenFromDateIsAfterToDate() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(null, null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1), null, null));

        assertEquals("The 'from' date cannot be after the 'to' date", ex.getMessage());
    }
}
