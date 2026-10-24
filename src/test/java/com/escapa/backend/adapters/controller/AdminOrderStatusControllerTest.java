package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.CancelOrderUseCase;
import com.escapa.backend.application.usecase.FakeCompanySeatsPort;
import com.escapa.backend.application.usecase.FakeOrderAccessRevocationPort;
import com.escapa.backend.application.usecase.InMemoryOrderRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.RefundOrderUseCase;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP e controle de acesso ADMIN do cancelamento e reembolso de pedidos (US-28). */
class AdminOrderStatusControllerTest {

    private static final String CANCEL_URL = "/api/v1/admin/orders/{id}/cancel";
    private static final String REFUND_URL = "/api/v1/admin/orders/{id}/refund";

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryOrderRepositoryPort orders = new InMemoryOrderRepositoryPort();
    private final FakeCompanySeatsPort seats = new FakeCompanySeatsPort();

    private String adminId;
    private String studentId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        studentId = userRepository.save(new User("Aluno", "aluno@escapa.com", "hash", "STUDENT")).getId().toString();

        final AdminOrderStatusController controller = new AdminOrderStatusController(
                new CancelOrderUseCase(orders, new FakeOrderAccessRevocationPort(), seats, Clock.systemDefaultZone()),
                new RefundOrderUseCase(orders),
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

    private static void authenticate(String userId, String role) {
        final var user = new AuthenticatedUser(UUID.fromString(userId), "test@escapa.com", role, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    private UUID givenOrder(OrderStatus status) {
        final Order order = new Order();
        order.setType(OrderType.INDIVIDUAL);
        order.setItemType(OrderItemType.COURSE);
        order.setBuyerUserId(UUID.randomUUID());
        order.setCourseId(UUID.randomUUID());
        order.setStatus(status);
        return orders.save(order).getId();
    }

    private ResultActions cancel(UUID orderId, String body) throws Exception {
        return mockMvc.perform(post(CANCEL_URL, orderId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void shouldRejectRequestsWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(post(CANCEL_URL, givenOrder(OrderStatus.APPROVED)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid access token"));
    }

    @Test
    void shouldRejectNonAdminUsersWith403() throws Exception {
        final UUID orderId = givenOrder(OrderStatus.APPROVED);
        authenticate(studentId, "STUDENT");

        mockMvc.perform(post(CANCEL_URL, orderId))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(REFUND_URL, orderId))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCancelAnOrderAndReturnTheStatusHistory() throws Exception {
        final UUID orderId = givenOrder(OrderStatus.APPROVED);

        cancel(orderId, "{\"reason\": \"Pedido duplicado\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancelled successfully"))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusHistory.length()").value(1))
                .andExpect(jsonPath("$.data.statusHistory[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusHistory[0].changedBy").value(adminId))
                .andExpect(jsonPath("$.data.statusHistory[0].reason").value("Pedido duplicado"));
    }

    @Test
    void shouldCancelWithoutABody() throws Exception {
        final UUID orderId = givenOrder(OrderStatus.APPROVED);

        mockMvc.perform(post(CANCEL_URL, orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusHistory[0].reason").doesNotExist());
    }

    @Test
    void shouldRefundACancelledOrder() throws Exception {
        final UUID orderId = givenOrder(OrderStatus.CANCELLED);

        mockMvc.perform(post(REFUND_URL, orderId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Estorno confirmado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order refunded successfully"))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.statusHistory[0].reason").value("Estorno confirmado"));
    }

    @Test
    void shouldReturn409WhenTheTransitionIsNotAllowed() throws Exception {
        final UUID approved = givenOrder(OrderStatus.APPROVED);
        final UUID cancelled = givenOrder(OrderStatus.CANCELLED);

        mockMvc.perform(post(REFUND_URL, approved))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("cannot become REFUNDED")));
        mockMvc.perform(post(CANCEL_URL, cancelled))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("cannot become CANCELLED")));
    }

    @Test
    void shouldReturn409WithTheNumbersWhenSeatsAreInUse() throws Exception {
        final UUID companyId = UUID.randomUUID();
        seats.givenCompany(companyId, 10, 7);
        final Order order = new Order();
        order.setType(OrderType.CORPORATE);
        order.setItemType(OrderItemType.SEATS);
        order.setCompanyId(companyId);
        order.setBuyerUserId(UUID.randomUUID());
        order.setSeatsQuantity(5);
        order.setStatus(OrderStatus.APPROVED);
        final UUID orderId = orders.save(order).getId();

        cancel(orderId, "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("7 seat(s) in use")))
                .andExpect(jsonPath("$.message", containsString("release 2 seat(s)")));
    }

    @Test
    void shouldReturn404ForAnUnknownOrder() throws Exception {
        mockMvc.perform(post(CANCEL_URL, UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(REFUND_URL, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAReasonLongerThanTheLimitWith400() throws Exception {
        final UUID orderId = givenOrder(OrderStatus.APPROVED);

        cancel(orderId, "{\"reason\": \"" + "x".repeat(256) + "\"}")
                .andExpect(status().isBadRequest());
    }
}
