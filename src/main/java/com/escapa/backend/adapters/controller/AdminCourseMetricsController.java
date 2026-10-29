package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CourseMetricsResponse;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.model.CourseMetrics;
import com.escapa.backend.application.usecase.GetAdminCourseMetricsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin - Metrics", description = "Catalog intelligence and historical metrics for authenticated ADMIN users")
@RequestMapping("/api/v1/admin/metrics")
public class AdminCourseMetricsController {

    private final GetAdminCourseMetricsUseCase getAdminCourseMetricsUseCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminCourseMetricsController(
            GetAdminCourseMetricsUseCase getAdminCourseMetricsUseCase,
            AdminRequestGuard adminRequestGuard
    ) {
        this.getAdminCourseMetricsUseCase = getAdminCourseMetricsUseCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @GetMapping("/courses")
    @Operation(summary = "Get historical course enrollment ranking and category distribution")
    public ResponseEntity<ApiResponse<CourseMetricsResponse>> getCourseMetrics() {
        adminRequestGuard.requireAdmin(AuthenticationContext.requireUser().id());
        final CourseMetrics metrics = getAdminCourseMetricsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success(CourseMetricsResponse.from(metrics)));
    }
}

