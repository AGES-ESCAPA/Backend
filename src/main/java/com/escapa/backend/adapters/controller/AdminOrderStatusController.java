package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.OrderActionRequest;
import com.escapa.backend.adapters.dto.OrderStatusResponse;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.UserIdHeader;
import com.escapa.backend.application.usecase.CancelOrderUseCase;
import com.escapa.backend.application.usecase.RefundOrderUseCase;
import com.escapa.backend.domain.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Cancelamento e reembolso de pedidos (US-28). Exigem o header {@code X-User-Id} de um
 * ADMIN, no mesmo modelo de {@link AdminModuleController}; a ausência ou outro tipo
 * resulta em 403.
 */
@RestController
@Tag(name = "Admin - Orders", description = "Order cancellation and refund for ADMIN users (requires the X-User-Id header)")
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderStatusController {

    private final CancelOrderUseCase cancelOrderUseCase;
    private final RefundOrderUseCase refundOrderUseCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminOrderStatusController(
            CancelOrderUseCase cancelOrderUseCase,
            RefundOrderUseCase refundOrderUseCase,
            AdminRequestGuard adminRequestGuard
    ) {
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.refundOrderUseCase = refundOrderUseCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an approved order and revoke the access it granted")
    public ResponseEntity<ApiResponse<OrderStatusResponse>> cancel(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) OrderActionRequest request
    ) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        final OrderStatusResponse response =
                OrderStatusResponse.from(cancelOrderUseCase.execute(id, admin.getId(), reasonOf(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Order cancelled successfully"));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Register the refund of a cancelled order")
    public ResponseEntity<ApiResponse<OrderStatusResponse>> refund(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) OrderActionRequest request
    ) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        final OrderStatusResponse response =
                OrderStatusResponse.from(refundOrderUseCase.execute(id, admin.getId(), reasonOf(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Order refunded successfully"));
    }

    private static String reasonOf(OrderActionRequest request) {
        return request == null ? null : request.reason();
    }
}
