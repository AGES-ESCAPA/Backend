package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderSummary;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // O banco é compartilhado entre as classes de teste e pedidos não têm ON DELETE CASCADE:
    // sem esta limpeza, outros testes que fazem deleteAll em users/courses quebram.
    @AfterEach
    void cleanUpOrders() {
        jdbcTemplate.update("delete from orders");
    }

    private UUID givenUser(String role) {
        final String email = "comprador-" + UUID.randomUUID() + "@email.com";
        return userRepositoryPort.save(new User("Comprador", email, "hash", role)).getId();
    }

    private UUID givenCompany() {
        final UUID companyId = givenUser("COMPANY");
        jdbcTemplate.update(
                "insert into company (id, company_name, cnpj_id, company_email, phone, seats_total) values (?, ?, ?, ?, ?, 0)",
                companyId, "Hotel Teste LTDA", UUID.randomUUID().toString().substring(0, 14), "contato@hotel.com.br", "51988887777");
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

    @Test
    void shouldFindOrdersFilteredAndPaginatedNewestFirst() {
        final UUID buyerId = givenUser("STUDENT");
        final UUID courseId = givenCourse();
        final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        final Order order1 = individualCourseOrder(buyerId, courseId);
        order1.setCreatedAt(now.minusDays(2));
        final UUID order1Id = orderRepositoryPort.save(order1).getId();

        final Order order2 = individualCourseOrder(buyerId, courseId);
        order2.setCreatedAt(now.minusDays(1));
        final UUID order2Id = orderRepositoryPort.save(order2).getId();

        final PageResult<OrderSummary> result = orderRepositoryPort.findOrders(
                new OrderFilter(OrderType.INDIVIDUAL, courseId, null, null, OrderStatus.APPROVED), 0, 10);

        assertEquals(2, result.content().size());
        assertEquals(order2Id, result.content().get(0).id());
        assertEquals(order1Id, result.content().get(1).id());
        assertEquals("Comprador", result.content().get(0).requester());
        assertEquals("Marketing Digital para Hospitalidade", result.content().get(0).courseTitle());

        final LocalDate purchaseDay = now.minusDays(1).toLocalDate();
        final PageResult<OrderSummary> byDate = orderRepositoryPort.findOrders(
                new OrderFilter(null, courseId, purchaseDay, purchaseDay, null), 0, 10);

        assertEquals(1, byDate.content().size());
        assertEquals(order2Id, byDate.content().get(0).id());
    }

    @Test
    void shouldIncludeTheWholeLastDayAndExcludeTheNextDayWhenFilteringByDate() {
        final UUID courseId = givenCourse();
        final LocalDate day = LocalDate.of(2026, 3, 9);

        final Order lastMoment = individualCourseOrder(givenUser("STUDENT"), courseId);
        lastMoment.setCreatedAt(day.atTime(23, 59, 59, 999_999_000));
        final UUID lastMomentId = orderRepositoryPort.save(lastMoment).getId();

        final Order nextDay = individualCourseOrder(givenUser("STUDENT"), courseId);
        nextDay.setCreatedAt(day.plusDays(1).atStartOfDay());
        orderRepositoryPort.save(nextDay);

        final PageResult<OrderSummary> result = orderRepositoryPort.findOrders(
                new OrderFilter(null, courseId, day, day, null), 0, 10);

        assertEquals(List.of(lastMomentId), result.content().stream().map(OrderSummary::id).toList());
    }

    @Test
    void shouldNotRepeatOrdersAcrossPagesWhenTheyHaveTheSameCreationDate() {
        final UUID courseId = givenCourse();
        final LocalDateTime sameMoment = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        final List<UUID> savedIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final Order order = individualCourseOrder(givenUser("STUDENT"), courseId);
            order.setCreatedAt(sameMoment);
            savedIds.add(orderRepositoryPort.save(order).getId());
        }

        final List<UUID> pagedIds = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            pagedIds.addAll(orderRepositoryPort.findOrders(
                    new OrderFilter(null, courseId, null, null, null), page, 1)
                    .content().stream().map(OrderSummary::id).toList());
        }

        assertEquals(3, pagedIds.stream().distinct().count());
        assertTrue(pagedIds.containsAll(savedIds));
    }

    @Test
    void shouldFindOrderDetailsForIndividualBuyer() {
        final UUID buyerId = givenUser("STUDENT");
        jdbcTemplate.update(
                "insert into regular_users (user_id, cpf, phone) values (?, '12345678909', '51988889999')", buyerId);
        final UUID courseId = givenCourse();

        final Order saved = orderRepositoryPort.save(individualCourseOrder(buyerId, courseId));

        final OrderDetails details = orderRepositoryPort.findDetailsById(saved.getId()).orElseThrow();
        assertEquals(saved.getId(), details.id());
        assertEquals(OrderType.INDIVIDUAL, details.type());
        assertNotNull(details.buyer());
        assertEquals("Comprador", details.buyer().name());
        assertEquals("12345678909", details.buyer().document());
        assertEquals("51988889999", details.buyer().phone());
        assertEquals(courseId, details.item().courseId());
        assertEquals("Marketing Digital para Hospitalidade", details.item().courseTitle());
        assertNull(details.collaboratorsWithAccessCount());
    }

    @Test
    void shouldFindCorporateOrderDetailsWithCollaboratorsCount() {
        final UUID buyerId = givenUser("COMPANY");
        final UUID companyId = givenCompany();
        final UUID courseId = givenCourse();

        final UUID employeeId = givenUser("STUDENT");
        jdbcTemplate.update(
                "insert into users_company (user_id, company_id, role) values (?, ?, 'DEV')",
                employeeId, companyId);
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, certificate_issued) values (?, ?, false)",
                employeeId, courseId);

        // Colaborador com matricula vencida nao tem mais acesso e nao entra na conta.
        final UUID expiredEmployeeId = givenUser("STUDENT");
        jdbcTemplate.update(
                "insert into users_company (user_id, company_id, role) values (?, ?, 'DEV')",
                expiredEmployeeId, companyId);
        jdbcTemplate.update(
                "insert into user_courses (user_id, course_id, certificate_issued, dt_expiracao) values (?, ?, false, ?)",
                expiredEmployeeId, courseId, LocalDate.now().minusDays(1));

        final Order order = new Order();
        order.setBuyerUserId(buyerId);
        order.setCompanyId(companyId);
        order.setType(OrderType.CORPORATE);
        order.setItemType(OrderItemType.COURSE);
        order.setCourseId(courseId);
        order.setAmount(new BigDecimal("150.00"));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setInstallments(1);
        order.changeStatus(OrderStatus.APPROVED, null, null);

        final Order saved = orderRepositoryPort.save(order);

        final OrderDetails details = orderRepositoryPort.findDetailsById(saved.getId()).orElseThrow();
        assertEquals(OrderType.CORPORATE, details.type());
        assertEquals("Hotel Teste LTDA", details.buyer().name());
        assertEquals("contato@hotel.com.br", details.buyer().email());
        assertEquals("51988887777", details.buyer().phone());
        assertEquals(1, details.collaboratorsWithAccessCount());
    }
}
