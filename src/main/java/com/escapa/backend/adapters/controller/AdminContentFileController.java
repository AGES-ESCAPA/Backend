package com.escapa.backend.adapters.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ContentFileResponse;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.UploadContentFileUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Admin - Contents", description = "Lesson management for authenticated ADMIN users")
@RequestMapping("/api/v1/admin/contents")
public class AdminContentFileController {

    private final UploadContentFileUseCase uploadContentFileUseCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminContentFileController(
            UploadContentFileUseCase uploadContentFileUseCase,
            AdminRequestGuard adminRequestGuard
    ) {
        this.uploadContentFileUseCase = uploadContentFileUseCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a lesson file to storage and return its public URL")
    public ResponseEntity<ApiResponse<ContentFileResponse>> upload(
            @RequestParam("file") MultipartFile file
    ) {
        adminRequestGuard.requireAdmin(AuthenticationContext.requireUser().id());
        final String url = uploadContentFileUseCase.execute(
                file.getOriginalFilename(), openStream(file), file.getSize());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new ContentFileResponse(url)));
    }

    private InputStream openStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
