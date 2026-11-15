package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.AdminMetricsOverviewResponse;
import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.GetAdminMetricsOverviewUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin - Metrics", description = "Catalog intelligence and historical metrics for authenticated ADMIN users")
@RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController {
    private final GetAdminMetricsOverviewUseCase useCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminMetricsController(GetAdminMetricsOverviewUseCase useCase, AdminRequestGuard adminRequestGuard) {
        this.useCase = useCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get global student and partner company metrics")
    public ResponseEntity<ApiResponse<AdminMetricsOverviewResponse>> overview() {
        adminRequestGuard.requireAdmin(AuthenticationContext.requireUser().id());
        return ResponseEntity.ok(ApiResponse.success(AdminMetricsOverviewResponse.from(useCase.execute())));
    }
}
