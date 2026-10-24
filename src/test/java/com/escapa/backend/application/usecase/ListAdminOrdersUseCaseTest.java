package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListAdminOrdersUseCaseTest {

    private InMemoryOrderRepositoryPort repository;
    private ListAdminOrdersUseCase useCase;

    private static final UUID COURSE_A = UUID.randomUUID();
    private static final UUID COURSE_B = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepositoryPort();
        useCase = new ListAdminOrdersUseCase(repository);
    }

    private OrderSummary createSummary(
            OrderType type,
            OrderItemType itemType,
            UUID courseId,
            OrderStatus status,
            LocalDateTime purchaseDate
    ) {
        return new OrderSummary(
                UUID.randomUUID(),
                type == OrderType.INDIVIDUAL ? "Aluno Teste" : "Empresa Teste LTDA",
                type,
                courseId,
                courseId != null ? "Curso Teste" : null,
                itemType == OrderItemType.SEATS ? 10 : null,
                purchaseDate,
                PaymentMethod.CARD,
                new BigDecimal("150.00"),
                status
        );
    }

    @Test
    void shouldReturnEmptyPageWhenNoOrdersExist() {
        final PageResult<OrderSummary> result = useCase.execute(
                null, null, null, null, null, 0, 10);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void shouldReturnPaginatedOrdersOrderedNewestFirst() {
        final LocalDateTime now = LocalDateTime.now();
        final OrderSummary order1 = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, now.minusDays(3));
        final OrderSummary order2 = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, now.minusDays(1));
        final OrderSummary order3 = createSummary(OrderType.CORPORATE, OrderItemType.SEATS, null, OrderStatus.APPROVED, now.minusDays(2));

        repository.addSummary(order1);
        repository.addSummary(order2);
        repository.addSummary(order3);

        final PageResult<OrderSummary> page0 = useCase.execute(null, null, null, null, null, 0, 2);

        assertEquals(2, page0.content().size());
        assertEquals(order2.id(), page0.content().get(0).id());
        assertEquals(order3.id(), page0.content().get(1).id());
        assertEquals(3, page0.totalElements());
        assertEquals(2, page0.totalPages());

        final PageResult<OrderSummary> page1 = useCase.execute(null, null, null, null, null, 1, 2);
        assertEquals(1, page1.content().size());
        assertEquals(order1.id(), page1.content().get(0).id());
    }

    @Test
    void shouldFilterByType() {
        final LocalDateTime now = LocalDateTime.now();
        final OrderSummary individual = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, now);
        final OrderSummary corporate = createSummary(OrderType.CORPORATE, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, now);

        repository.addSummary(individual);
        repository.addSummary(corporate);

        final PageResult<OrderSummary> result = useCase.execute(
                OrderType.INDIVIDUAL, null, null, null, null, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(individual.id(), result.content().get(0).id());
    }

    @Test
    void shouldFilterByCourseId() {
        final LocalDateTime now = LocalDateTime.now();
        final OrderSummary orderCourseA = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, now);
        final OrderSummary orderCourseB = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_B, OrderStatus.APPROVED, now);

        repository.addSummary(orderCourseA);
        repository.addSummary(orderCourseB);

        final PageResult<OrderSummary> result = useCase.execute(
                null, COURSE_A, null, null, null, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(orderCourseA.id(), result.content().get(0).id());
    }

    @Test
    void shouldFilterByStatus() {
        final LocalDateTime now = LocalDateTime.now();
        final OrderSummary approved = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, now);
        final OrderSummary cancelled = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.CANCELLED, now);
        final OrderSummary refunded = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.REFUNDED, now);

        repository.addSummary(approved);
        repository.addSummary(cancelled);
        repository.addSummary(refunded);

        final PageResult<OrderSummary> result = useCase.execute(
                null, null, null, null, OrderStatus.CANCELLED, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(cancelled.id(), result.content().get(0).id());
    }

    @Test
    void shouldFilterByDateRange() {
        final OrderSummary febOrder = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, LocalDateTime.of(2026, 2, 15, 10, 0));
        final OrderSummary marOrder1 = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, LocalDateTime.of(2026, 3, 5, 14, 30));
        final OrderSummary marOrder2 = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, LocalDateTime.of(2026, 3, 20, 9, 15));
        final OrderSummary aprOrder = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, LocalDateTime.of(2026, 4, 1, 8, 0));

        repository.addSummary(febOrder);
        repository.addSummary(marOrder1);
        repository.addSummary(marOrder2);
        repository.addSummary(aprOrder);

        final PageResult<OrderSummary> result = useCase.execute(
                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, 0, 10);

        assertEquals(2, result.content().size());
        assertEquals(marOrder2.id(), result.content().get(0).id());
        assertEquals(marOrder1.id(), result.content().get(1).id());
    }

    @Test
    void shouldFilterByCombinedFilters() {
        final OrderSummary matching = createSummary(OrderType.CORPORATE, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, LocalDateTime.of(2026, 3, 10, 10, 0));
        final OrderSummary diffType = createSummary(OrderType.INDIVIDUAL, OrderItemType.COURSE, COURSE_A, OrderStatus.APPROVED, LocalDateTime.of(2026, 3, 10, 10, 0));
        final OrderSummary diffCourse = createSummary(OrderType.CORPORATE, OrderItemType.COURSE, COURSE_B, OrderStatus.APPROVED, LocalDateTime.of(2026, 3, 10, 10, 0));
        final OrderSummary diffStatus = createSummary(OrderType.CORPORATE, OrderItemType.COURSE, COURSE_A, OrderStatus.CANCELLED, LocalDateTime.of(2026, 3, 10, 10, 0));

        repository.addSummary(matching);
        repository.addSummary(diffType);
        repository.addSummary(diffCourse);
        repository.addSummary(diffStatus);

        final PageResult<OrderSummary> result = useCase.execute(
                OrderType.CORPORATE, COURSE_A, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), OrderStatus.APPROVED, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(matching.id(), result.content().get(0).id());
    }

    @Test
    void shouldThrowExceptionWhenFromDateIsAfterToDate() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(null, null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1), null, 0, 10));

        assertEquals("The 'from' date cannot be after the 'to' date", ex.getMessage());
    }
}
