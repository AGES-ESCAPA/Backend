package com.escapa.backend.adapters.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.GetAdminOrderStatsUseCase;
import com.escapa.backend.application.usecase.GetAdminOrderUseCase;
import com.escapa.backend.application.usecase.InMemoryOrderRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.ListAdminOrdersUseCase;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;

class AdminOrderControllerTest {

    private static final String USER_HEADER = "Authorization";
    private static final String ORDERS_URL = "/api/v1/admin/orders";

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryOrderRepositoryPort orderRepository = new InMemoryOrderRepositoryPort();

    private String adminId;
    private String studentId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(new User("Admin Escapa", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        studentId = userRepository.save(new User("Aluno Escapa", "aluno@escapa.com", "hash", "STUDENT")).getId().toString();

        final AdminOrderController controller = new AdminOrderController(
                new ListAdminOrdersUseCase(orderRepository),
                new GetAdminOrderUseCase(orderRepository),
                new GetAdminOrderStatsUseCase(orderRepository),
                new AdminRequestGuard(new AuthorizeAdminUseCase(userRepository))
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate(adminId, "ADMIN");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectRequestsWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get(ORDERS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid access token"));
    }

    @Test
    void shouldRejectNonAdminUsers() throws Exception {
        authenticate(studentId, "STUDENT");
        mockMvc.perform(get(ORDERS_URL).header(USER_HEADER, "Bearer student-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: user is not ADMIN"));
    }

    private static void authenticate(String userId, String role) {
        final var user = new AuthenticatedUser(UUID.fromString(userId), "test@escapa.com", role, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Test
    void shouldListOrdersSuccessfully() throws Exception {
        final UUID orderId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.now();

        orderRepository.addSummary(new OrderSummary(
                orderId,
                "Mariana Costa",
                OrderType.INDIVIDUAL,
                courseId,
                "Turismo Rural",
                null,
                now,
                PaymentMethod.CARD,
                new BigDecimal("97.00"),
                OrderStatus.APPROVED
        ));

        mockMvc.perform(get(ORDERS_URL).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.content[0].requester").value("Mariana Costa"))
                .andExpect(jsonPath("$.data.content[0].type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.data.content[0].courseTitle").value("Turismo Rural"))
                .andExpect(jsonPath("$.data.content[0].amount").value(97.00))
                .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageNumber").value(0));
    }

    @Test
    void shouldReturnOrderStatsForTheFilters() throws Exception {
        final LocalDateTime now = LocalDateTime.now();
        orderRepository.addSummary(statsSummary(OrderType.INDIVIDUAL, OrderStatus.APPROVED, "97.00", now));
        orderRepository.addSummary(statsSummary(OrderType.INDIVIDUAL, OrderStatus.APPROVED, "150.50", now));
        orderRepository.addSummary(statsSummary(OrderType.INDIVIDUAL, OrderStatus.REFUNDED, "80.00", now));
        orderRepository.addSummary(statsSummary(OrderType.CORPORATE, OrderStatus.APPROVED, "990.00", now));

        mockMvc.perform(get(ORDERS_URL + "/stats").header(USER_HEADER, adminId).param("type", "INDIVIDUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvedCount").value(2))
                .andExpect(jsonPath("$.data.approvedRevenue").value(247.50))
                .andExpect(jsonPath("$.data.cancelledOrRefundedCount").value(1));
    }

    @Test
    void shouldSearchOrdersBeyondTheCurrentPage() throws Exception {
        final LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 3; i++) {
            orderRepository.addSummary(statsSummary(OrderType.INDIVIDUAL, OrderStatus.APPROVED, "97.00", now));
        }
        final OrderSummary target = new OrderSummary(
                UUID.randomUUID(), "Pousada Vista Mar LTDA", OrderType.CORPORATE, null, null, 10,
                now.minusDays(10), PaymentMethod.CARD, new BigDecimal("990.00"), OrderStatus.APPROVED);
        orderRepository.addSummary(target);

        mockMvc.perform(get(ORDERS_URL).header(USER_HEADER, adminId)
                        .param("search", "vista mar").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(target.id().toString()));

        mockMvc.perform(get(ORDERS_URL + "/stats").header(USER_HEADER, adminId).param("search", "vista mar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvedCount").value(1))
                .andExpect(jsonPath("$.data.approvedRevenue").value(990.00));
    }

    @Test
    void shouldRejectNonAdminUsersOnStats() throws Exception {
        authenticate(studentId, "STUDENT");
        mockMvc.perform(get(ORDERS_URL + "/stats").header(USER_HEADER, "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    private static OrderSummary statsSummary(OrderType type, OrderStatus status, String amount, LocalDateTime date) {
        return new OrderSummary(
                UUID.randomUUID(), "Comprador", type, UUID.randomUUID(), "Curso", null,
                date, PaymentMethod.CARD, new BigDecimal(amount), status);
    }

    @Test
    void shouldGetOrderDetailsSuccessfully() throws Exception {
        final UUID orderId = UUID.randomUUID();
        final UUID buyerId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.now();

        final OrderDetails details = new OrderDetails(
                orderId,
                OrderType.CORPORATE,
                new OrderDetails.Buyer(buyerId, "Pousada Vista Mar LTDA", "rh@vistamar.com.br", "12345678000191", "51999990001"),
                new OrderDetails.Item(OrderItemType.COURSE, courseId, "Inovação em Destinos", null),
                new BigDecimal("97.00"),
                PaymentMethod.CARD,
                1,
                OrderStatus.APPROVED,
                now,
                List.of(new OrderStatusChange(UUID.randomUUID(), OrderStatus.APPROVED, null, null, now)),
                4
        );
        orderRepository.addDetails(details);

        mockMvc.perform(get(ORDERS_URL + "/{id}", orderId).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.type").value("CORPORATE"))
                .andExpect(jsonPath("$.data.buyer.name").value("Pousada Vista Mar LTDA"))
                .andExpect(jsonPath("$.data.buyer.document").value("12345678000191"))
                .andExpect(jsonPath("$.data.buyer.phone").value("51999990001"))
                .andExpect(jsonPath("$.data.item.courseTitle").value("Inovação em Destinos"))
                .andExpect(jsonPath("$.data.collaboratorsWithAccessCount").value(4))
                .andExpect(jsonPath("$.data.statusHistory[0].status").value("APPROVED"));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        final UUID randomId = UUID.randomUUID();

        mockMvc.perform(get(ORDERS_URL + "/{id}", randomId).header(USER_HEADER, adminId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found: " + randomId));
    }

    @Test
    void shouldReturn400WhenFromDateIsAfterToDate() throws Exception {
        mockMvc.perform(get(ORDERS_URL)
                        .header(USER_HEADER, adminId)
                        .param("from", "2026-04-01")
                        .param("to", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("The 'from' date cannot be after the 'to' date"));
    }
}
