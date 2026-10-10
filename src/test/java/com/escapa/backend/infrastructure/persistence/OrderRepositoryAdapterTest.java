package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private OrderRepositoryPort orderRepositoryPort;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    // company nao tem repositorio JPA proprio para escrita nos testes: grava direto na tabela.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID givenUser(String role) {
        final String email = "comprador-" + UUID.randomUUID() + "@email.com";
        return userRepositoryPort.save(new User("Comprador", email, "hash", role)).getId();
    }

    private UUID givenCompany() {
        final UUID companyId = givenUser("COMPANY");
        jdbcTemplate.update(
                "insert into company (id, company_name, cnpj_id, seats_total) values (?, ?, ?, 0)",
                companyId, "Hotel Teste LTDA", UUID.randomUUID().toString().substring(0, 14));
        return companyId;
    }

    private UUID givenCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Marketing Digital para Hospitalidade");
        return courseJpaRepository.save(course).getId();
    }

    private Order individualCourseOrder(UUID buyerId, UUID courseId) {
        final Order order = new Order();
        order.setBuyerUserId(buyerId);
        order.setType(OrderType.INDIVIDUAL);
        order.setItemType(OrderItemType.COURSE);
        order.setCourseId(courseId);
        order.setAmount(new BigDecimal("117.00"));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setInstallments(3);
        order.changeStatus(OrderStatus.APPROVED, null, "Pagamento aprovado");
        return order;
    }

    @Test
    void shouldSaveAndReadAnIndividualCourseOrderWithItsHistory() {
        final UUID buyerId = givenUser("STUDENT");
        final UUID courseId = givenCourse();

        final Order saved = orderRepositoryPort.save(individualCourseOrder(buyerId, courseId));

        assertNotNull(saved.getId());
        final Optional<Order> found = orderRepositoryPort.findById(saved.getId());
        assertTrue(found.isPresent());
        final Order order = found.get();
        assertEquals(buyerId, order.getBuyerUserId());
        assertNull(order.getCompanyId());
        assertEquals(OrderType.INDIVIDUAL, order.getType());
        assertEquals(OrderItemType.COURSE, order.getItemType());
        assertEquals(courseId, order.getCourseId());
        assertNull(order.getSeatsQuantity());
        assertEquals(0, new BigDecimal("117.00").compareTo(order.getAmount()));
        assertEquals(PaymentMethod.CARD, order.getPaymentMethod());
        assertEquals(3, order.getInstallments());
        assertEquals(OrderStatus.APPROVED, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertEquals(1, order.getStatusHistory().size());
        final OrderStatusChange change = order.getStatusHistory().get(0);
        assertNotNull(change.id());
        assertEquals(OrderStatus.APPROVED, change.status());
        assertEquals("Pagamento aprovado", change.reason());
        assertNull(change.changedBy());
    }

    @Test
    void shouldSaveAndReadACorporateSeatsOrder() {
        final UUID buyerId = givenUser("COMPANY");
        final UUID companyId = givenCompany();
        final Order order = new Order();
        order.setBuyerUserId(buyerId);
        order.setCompanyId(companyId);
        order.setType(OrderType.CORPORATE);
        order.setItemType(OrderItemType.SEATS);
        order.setSeatsQuantity(10);
        order.setAmount(new BigDecimal("990.00"));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setInstallments(1);
        order.changeStatus(OrderStatus.APPROVED, null, null);

        final Order saved = orderRepositoryPort.save(order);

        final Order found = orderRepositoryPort.findById(saved.getId()).orElseThrow();
        assertEquals(OrderType.CORPORATE, found.getType());
        assertEquals(OrderItemType.SEATS, found.getItemType());
        assertEquals(companyId, found.getCompanyId());
        assertEquals(10, found.getSeatsQuantity());
        assertNull(found.getCourseId());
    }

    @Test
    void shouldAppendStatusChangesToTheHistoryInOrder() {
        final UUID adminId = givenUser("ADMIN");
        final Order saved = orderRepositoryPort.save(individualCourseOrder(givenUser("STUDENT"), givenCourse()));

        final Order loaded = orderRepositoryPort.findById(saved.getId()).orElseThrow();
        loaded.changeStatus(OrderStatus.REFUNDED, adminId, "Reembolso solicitado pelo aluno");
        orderRepositoryPort.save(loaded);

        final Order found = orderRepositoryPort.findById(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.REFUNDED, found.getStatus());
        final List<OrderStatus> statuses = found.getStatusHistory().stream()
                .map(OrderStatusChange::status)
                .toList();
        assertEquals(List.of(OrderStatus.APPROVED, OrderStatus.REFUNDED), statuses);
        final OrderStatusChange refund = found.getStatusHistory().get(1);
        assertEquals(adminId, refund.changedBy());
        assertEquals("Reembolso solicitado pelo aluno", refund.reason());
    }

    @Test
    void shouldListTheBuyerOrdersNewestFirst() {
        final UUID buyerId = givenUser("STUDENT");
        final LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        final List<UUID> savedIds = new ArrayList<>();
        for (int day = 0; day < 3; day++) {
            final Order order = individualCourseOrder(buyerId, givenCourse());
            order.setCreatedAt(base.minusDays(day));
            savedIds.add(orderRepositoryPort.save(order).getId());
        }
        orderRepositoryPort.save(individualCourseOrder(givenUser("STUDENT"), givenCourse()));

        final List<Order> orders = orderRepositoryPort.findByBuyerUserId(buyerId);

        assertEquals(savedIds, orders.stream().map(Order::getId).toList());
        assertTrue(orders.stream().allMatch(order -> order.getStatusHistory().size() == 1));
    }

    @Test
    void shouldReturnEmptyWhenTheOrderDoesNotExist() {
        assertTrue(orderRepositoryPort.findById(UUID.randomUUID()).isEmpty());
    }
}
