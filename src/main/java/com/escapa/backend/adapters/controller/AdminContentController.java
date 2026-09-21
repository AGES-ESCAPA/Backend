package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ContentResponse;
import com.escapa.backend.adapters.dto.CreateContentRequest;
import com.escapa.backend.adapters.dto.ReorderContentsRequest;
import com.escapa.backend.adapters.dto.UpdateContentRequest;
import com.escapa.backend.application.usecase.CreateContentUseCase;
import com.escapa.backend.application.usecase.DeleteContentUseCase;
import com.escapa.backend.application.usecase.GetContentUseCase;
import com.escapa.backend.application.usecase.ListModuleContentsUseCase;
import com.escapa.backend.application.usecase.ReorderContentsUseCase;
import com.escapa.backend.application.usecase.UpdateContentUseCase;
import com.escapa.backend.domain.entity.Content;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminContentController {

    private final CreateContentUseCase createContentUseCase;
    private final UpdateContentUseCase updateContentUseCase;
    private final GetContentUseCase getContentUseCase;
    private final ListModuleContentsUseCase listModuleContentsUseCase;
    private final DeleteContentUseCase deleteContentUseCase;
    private final ReorderContentsUseCase reorderContentsUseCase;

    public AdminContentController(
            CreateContentUseCase createContentUseCase,
            UpdateContentUseCase updateContentUseCase,
            GetContentUseCase getContentUseCase,
            ListModuleContentsUseCase listModuleContentsUseCase,
            DeleteContentUseCase deleteContentUseCase,
            ReorderContentsUseCase reorderContentsUseCase
    ) {
        this.createContentUseCase = createContentUseCase;
        this.updateContentUseCase = updateContentUseCase;
        this.getContentUseCase = getContentUseCase;
        this.listModuleContentsUseCase = listModuleContentsUseCase;
        this.deleteContentUseCase = deleteContentUseCase;
        this.reorderContentsUseCase = reorderContentsUseCase;
    }

    @PostMapping("/modules/{moduleId}/contents")
    public ResponseEntity<ApiResponse<ContentResponse>> create(
            @PathVariable UUID moduleId,
            @Valid @RequestBody CreateContentRequest request
    ) {
        final Content content = createContentUseCase.execute(
                moduleId,
                request.title(),
                request.type(),
                request.url(),
                request.durationMinutes(),
                request.description(),
                request.isFree()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ContentResponse.from(content), "Content created successfully"));
    }

    @GetMapping("/modules/{moduleId}/contents")
    public ResponseEntity<ApiResponse<List<ContentResponse>>> listByModule(@PathVariable UUID moduleId) {
        final List<ContentResponse> contents = listModuleContentsUseCase.execute(moduleId).stream()
                .map(ContentResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(contents));
    }

    @GetMapping("/modules/{moduleId}/contents/{id}")
    public ResponseEntity<ApiResponse<ContentResponse>> getById(
            @PathVariable UUID moduleId,
            @PathVariable UUID id
    ) {
        final Content content = getContentUseCase.execute(moduleId, id);
        return ResponseEntity.ok(ApiResponse.success(ContentResponse.from(content)));
    }

    @PutMapping("/contents/{id}")
    public ResponseEntity<ApiResponse<ContentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentRequest request
    ) {
        final Content content = updateContentUseCase.execute(
                id,
                request.title(),
                request.type(),
                request.url(),
                request.durationMinutes(),
                request.description(),
                request.isFree()
        );
        return ResponseEntity.ok(ApiResponse.success(ContentResponse.from(content), "Content updated successfully"));
    }

    @PutMapping("/modules/{moduleId}/contents/reorder")
    public ResponseEntity<ApiResponse<List<ContentResponse>>> reorder(
            @PathVariable UUID moduleId,
            @Valid @RequestBody ReorderContentsRequest request
    ) {
        final List<ContentResponse> contents = reorderContentsUseCase
                .execute(moduleId, request.contentIds())
                .stream()
                .map(ContentResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(contents, "Contents reordered successfully"));
    }

    @DeleteMapping("/contents/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteContentUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
