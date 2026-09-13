package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CreateModuleRequest;
import com.escapa.backend.adapters.dto.ModuleResponse;
import com.escapa.backend.adapters.dto.ReorderModulesRequest;
import com.escapa.backend.adapters.dto.UpdateModuleRequest;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.CreateModuleUseCase;
import com.escapa.backend.application.usecase.DeleteModuleUseCase;
import com.escapa.backend.application.usecase.ListCourseModulesUseCase;
import com.escapa.backend.application.usecase.ReorderModulesUseCase;
import com.escapa.backend.application.usecase.UpdateModuleUseCase;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.entity.User;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * CRUD administrativo de modulos (US-06). Todos os endpoints exigem o header
 * {@code X-User-Id} de um usuario com userType ADMIN, no mesmo modelo de
 * {@link AdminCourseController}; a ausencia ou outro tipo resulta em 403.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminModuleController {

    private final ListCourseModulesUseCase listCourseModulesUseCase;
    private final CreateModuleUseCase createModuleUseCase;
    private final UpdateModuleUseCase updateModuleUseCase;
    private final ReorderModulesUseCase reorderModulesUseCase;
    private final DeleteModuleUseCase deleteModuleUseCase;
    private final UserRepositoryPort userRepositoryPort;

    public AdminModuleController(
            ListCourseModulesUseCase listCourseModulesUseCase,
            CreateModuleUseCase createModuleUseCase,
            UpdateModuleUseCase updateModuleUseCase,
            ReorderModulesUseCase reorderModulesUseCase,
            DeleteModuleUseCase deleteModuleUseCase,
            UserRepositoryPort userRepositoryPort
    ) {
        this.listCourseModulesUseCase = listCourseModulesUseCase;
        this.createModuleUseCase = createModuleUseCase;
        this.updateModuleUseCase = updateModuleUseCase;
        this.reorderModulesUseCase = reorderModulesUseCase;
        this.deleteModuleUseCase = deleteModuleUseCase;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping("/courses/{courseId}/modules")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> listByCourse(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId
    ) {
        requireAdmin(xUserId);
        final List<ModuleResponse> modules = listCourseModulesUseCase.execute(courseId).stream()
                .map(ModuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(modules));
    }

    @PostMapping("/courses/{courseId}/modules")
    public ResponseEntity<ApiResponse<ModuleResponse>> create(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateModuleRequest request
    ) {
        requireAdmin(xUserId);
        final Module module = createModuleUseCase.execute(courseId, request.title());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ModuleResponse.from(module), "Module created successfully"));
    }

    @PutMapping("/modules/{id}")
    public ResponseEntity<ApiResponse<ModuleResponse>> update(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateModuleRequest request
    ) {
        requireAdmin(xUserId);
        final Module module = updateModuleUseCase.execute(id, request.title());
        return ResponseEntity.ok(ApiResponse.success(ModuleResponse.from(module), "Module updated successfully"));
    }

    @PutMapping("/courses/{courseId}/modules/reorder")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> reorder(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderModulesRequest request
    ) {
        requireAdmin(xUserId);
        final List<ModuleResponse> modules = reorderModulesUseCase
                .execute(courseId, request.moduleIds())
                .stream()
                .map(ModuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(modules, "Modules reordered successfully"));
    }

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID id
    ) {
        requireAdmin(xUserId);
        deleteModuleUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    private User requireAdmin(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing X-User-Id header");
        }
        try {
            final UUID userId = UUID.fromString(xUserId);
            final User user = userRepositoryPort.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
            if (!"ADMIN".equalsIgnoreCase(user.getUserType())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: user is not ADMIN");
            }
            return user;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid X-User-Id: must be a valid UUID");
        }
    }
}
