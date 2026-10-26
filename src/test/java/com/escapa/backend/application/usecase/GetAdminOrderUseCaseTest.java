package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderNotFoundException;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetAdminOrderUseCaseTest {

    private InMemoryOrderRepositoryPort repository;
    private GetAdminOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepositoryPort();
        useCase = new GetAdminOrderUseCase(repository);
    }

    @Test
    void shouldReturnIndividualOrderDetails() {
        final UUID orderId = UUID.randomUUID();
        final UUID buyerId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.now();

        final OrderDetails details = new OrderDetails(
                orderId,
                OrderType.INDIVIDUAL,
                new OrderDetails.Buyer(buyerId, "Mariana Costa", "mariana@email.com", "12345678900", "51999991234"),
                new OrderDetails.Item(OrderItemType.COURSE, courseId, "Excelência no Atendimento", null),
                new BigDecimal("199.90"),
                PaymentMethod.CARD,
                2,
                OrderStatus.APPROVED,
                now,
                List.of(new OrderStatusChange(UUID.randomUUID(), OrderStatus.APPROVED, null, null, now)),
                null
        );

        repository.addDetails(details);

        final OrderDetails result = useCase.execute(orderId);

        assertEquals(orderId, result.id());
        assertEquals(OrderType.INDIVIDUAL, result.type());
        assertNotNull(result.buyer());
        assertEquals("Mariana Costa", result.buyer().name());
        assertEquals("12345678900", result.buyer().document());
        assertEquals("Excelência no Atendimento", result.item().courseTitle());
        assertNull(result.collaboratorsWithAccessCount());
        assertEquals(1, result.statusHistory().size());
        assertEquals(OrderStatus.APPROVED, result.statusHistory().get(0).status());
    }

    @Test
    void shouldReturnCorporateCourseOrderDetailsWithCollaboratorsCount() {
        final UUID orderId = UUID.randomUUID();
        final UUID companyId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.now();

        final OrderDetails details = new OrderDetails(
                orderId,
                OrderType.CORPORATE,
                new OrderDetails.Buyer(companyId, "Pousada Vista Mar LTDA", "contato@vistamar.com.br", "12345678000191", "51999990001"),
                new OrderDetails.Item(OrderItemType.COURSE, courseId, "Inovação em Destinos", null),
                new BigDecimal("97.00"),
                PaymentMethod.CARD,
                1,
                OrderStatus.APPROVED,
                now,
                List.of(new OrderStatusChange(UUID.randomUUID(), OrderStatus.APPROVED, null, null, now)),
                5
        );

        repository.addDetails(details);

        final OrderDetails result = useCase.execute(orderId);

        assertEquals(orderId, result.id());
        assertEquals(OrderType.CORPORATE, result.type());
        assertEquals("Pousada Vista Mar LTDA", result.buyer().name());
        assertEquals("12345678000191", result.buyer().document());
        assertEquals(5, result.collaboratorsWithAccessCount());
    }

    @Test
    void shouldReturnCorporateSeatsOrderDetailsWithoutCollaboratorsCount() {
        final UUID orderId = UUID.randomUUID();
        final UUID companyId = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.now();

        final OrderDetails details = new OrderDetails(
                orderId,
                OrderType.CORPORATE,
                new OrderDetails.Buyer(companyId, "Hotel Teste LTDA", "rh@hotel.com.br", "99887766000155", "51988887777"),
                new OrderDetails.Item(OrderItemType.SEATS, null, null, 15),
                new BigDecimal("1185.00"),
                PaymentMethod.CARD,
                3,
                OrderStatus.APPROVED,
                now,
                List.of(new OrderStatusChange(UUID.randomUUID(), OrderStatus.APPROVED, null, null, now)),
                null
        );

        repository.addDetails(details);

        final OrderDetails result = useCase.execute(orderId);

        assertEquals(orderId, result.id());
        assertEquals(15, result.item().seatsQuantity());
        assertNull(result.item().courseId());
        assertNull(result.collaboratorsWithAccessCount());
    }

    @Test
    void shouldReturnStatusHistoryInChronologicalOrder() {
        final UUID orderId = UUID.randomUUID();
        final LocalDateTime t1 = LocalDateTime.now().minusHours(2);
        final LocalDateTime t2 = LocalDateTime.now().minusHours(1);

        final OrderDetails details = new OrderDetails(
                orderId,
                OrderType.INDIVIDUAL,
                new OrderDetails.Buyer(UUID.randomUUID(), "Aluno", "aluno@email.com", "111", "222"),
                new OrderDetails.Item(OrderItemType.COURSE, UUID.randomUUID(), "Curso", null),
                new BigDecimal("100.00"),
                PaymentMethod.CARD,
                1,
                OrderStatus.CANCELLED,
                t1,
                List.of(
                        new OrderStatusChange(UUID.randomUUID(), OrderStatus.APPROVED, null, null, t1),
                        new OrderStatusChange(UUID.randomUUID(), OrderStatus.CANCELLED, UUID.randomUUID(), "Solicitado", t2)
                ),
                null
        );

        repository.addDetails(details);

        final OrderDetails result = useCase.execute(orderId);

        assertEquals(2, result.statusHistory().size());
        assertEquals(OrderStatus.APPROVED, result.statusHistory().get(0).status());
        assertEquals(OrderStatus.CANCELLED, result.statusHistory().get(1).status());
    }

    @Test
    void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
        final UUID randomId = UUID.randomUUID();

        final OrderNotFoundException ex = assertThrows(OrderNotFoundException.class, () ->
                useCase.execute(randomId));

        assertEquals("Order not found: " + randomId, ex.getMessage());
    }
}

