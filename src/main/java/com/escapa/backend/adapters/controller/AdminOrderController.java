package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.AdminOrderDetailsResponse;
import com.escapa.backend.adapters.dto.AdminOrderListItemResponse;
import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.application.usecase.GetAdminOrderUseCase;
import com.escapa.backend.application.usecase.ListAdminOrdersUseCase;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@Tag(name = "Admin - Orders", description = "Order management for authenticated ADMIN users")
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final ListAdminOrdersUseCase listAdminOrdersUseCase;
    private final GetAdminOrderUseCase getAdminOrderUseCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminOrderController(
            ListAdminOrdersUseCase listAdminOrdersUseCase,
            GetAdminOrderUseCase getAdminOrderUseCase,
            AdminRequestGuard adminRequestGuard
    ) {
        this.listAdminOrdersUseCase = listAdminOrdersUseCase;
        this.getAdminOrderUseCase = getAdminOrderUseCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @GetMapping
    @Operation(summary = "List and filter orders for administration")
    public ResponseEntity<ApiResponse<PageResponse<AdminOrderListItemResponse>>> list(
            @RequestParam(required = false) OrderType type,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = PaginationDefaults.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.PAGE_SIZE) int size
    ) {
        requireAdmin();
        final PageResult<OrderSummary> result = listAdminOrdersUseCase.execute(
                type, courseId, from, to, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result, AdminOrderListItemResponse::from)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID for administration")
    public ResponseEntity<ApiResponse<AdminOrderDetailsResponse>> getById(@PathVariable UUID id) {
        requireAdmin();
        final OrderDetails details = getAdminOrderUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(AdminOrderDetailsResponse.from(details)));
    }

    private void requireAdmin() {
        adminRequestGuard.requireAdmin(AuthenticationContext.requireUser().id());
    }
}
