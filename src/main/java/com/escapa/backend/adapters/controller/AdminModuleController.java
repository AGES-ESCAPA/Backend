package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ModuleRequest;
import com.escapa.backend.adapters.dto.ModuleResponse;
import com.escapa.backend.adapters.dto.ReorderModulesRequest;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.UserIdHeader;
import com.escapa.backend.application.usecase.CreateModuleUseCase;
import com.escapa.backend.application.usecase.DeleteModuleUseCase;
import com.escapa.backend.application.usecase.ListCourseModulesUseCase;
import com.escapa.backend.application.usecase.ReorderModulesUseCase;
import com.escapa.backend.application.usecase.UpdateModuleUseCase;
import com.escapa.backend.domain.entity.Module;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * CRUD administrativo de modulos (US-06). Todos os endpoints exigem o header
 * {@code X-User-Id} de um usuario com userType ADMIN, no mesmo modelo de
 * {@link AdminCourseController}; a ausencia ou outro tipo resulta em 403.
 */
@RestController
@Tag(name = "Admin - Modules", description = "Course module management for ADMIN users (requires the X-User-Id header)")
@RequestMapping("/api/v1/admin")
public class AdminModuleController {

    private final ListCourseModulesUseCase listCourseModulesUseCase;
    private final CreateModuleUseCase createModuleUseCase;
    private final UpdateModuleUseCase updateModuleUseCase;
    private final ReorderModulesUseCase reorderModulesUseCase;
    private final DeleteModuleUseCase deleteModuleUseCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminModuleController(
            ListCourseModulesUseCase listCourseModulesUseCase,
            CreateModuleUseCase createModuleUseCase,
            UpdateModuleUseCase updateModuleUseCase,
            ReorderModulesUseCase reorderModulesUseCase,
            DeleteModuleUseCase deleteModuleUseCase,
            AdminRequestGuard adminRequestGuard
    ) {
        this.listCourseModulesUseCase = listCourseModulesUseCase;
        this.createModuleUseCase = createModuleUseCase;
        this.updateModuleUseCase = updateModuleUseCase;
        this.reorderModulesUseCase = reorderModulesUseCase;
        this.deleteModuleUseCase = deleteModuleUseCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @GetMapping("/courses/{courseId}/modules")
    @Operation(summary = "List the modules of a course")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> listByCourse(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId
    ) {
        adminRequestGuard.requireAdmin(xUserId);
        final List<ModuleResponse> modules = listCourseModulesUseCase.execute(courseId).stream()
                .map(ModuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(modules));
    }

    @PostMapping("/courses/{courseId}/modules")
    @Operation(summary = "Create a module at the end of a course")
    public ResponseEntity<ApiResponse<ModuleResponse>> create(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleRequest request
    ) {
        adminRequestGuard.requireAdmin(xUserId);
        final Module module = createModuleUseCase.execute(courseId, request.title());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ModuleResponse.from(module), "Module created successfully"));
    }

    @PutMapping("/modules/{id}")
    @Operation(summary = "Rename a module")
    public ResponseEntity<ApiResponse<ModuleResponse>> update(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID id,
            @Valid @RequestBody ModuleRequest request
    ) {
        adminRequestGuard.requireAdmin(xUserId);
        final Module module = updateModuleUseCase.execute(id, request.title());
        return ResponseEntity.ok(ApiResponse.success(ModuleResponse.from(module), "Module updated successfully"));
    }

    @PutMapping("/courses/{courseId}/modules/reorder")
    @Operation(summary = "Reorder the modules of a course")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> reorder(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderModulesRequest request
    ) {
        adminRequestGuard.requireAdmin(xUserId);
        final List<ModuleResponse> modules = reorderModulesUseCase
                .execute(courseId, request.moduleIds())
                .stream()
                .map(ModuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(modules, "Modules reordered successfully"));
    }

    @DeleteMapping("/modules/{id}")
    @Operation(summary = "Delete a module")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID id
    ) {
        adminRequestGuard.requireAdmin(xUserId);
        deleteModuleUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

}
