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
import com.escapa.backend.adapters.dto.course.CourseThumbnailResponse;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.UploadCourseThumbnailUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Admin - Courses", description = "Course cover upload for authenticated ADMIN users")
@RequestMapping("/api/v1/admin/courses/thumbnail")
public class AdminCourseThumbnailController {

    private final UploadCourseThumbnailUseCase uploadCourseThumbnailUseCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminCourseThumbnailController(
            UploadCourseThumbnailUseCase uploadCourseThumbnailUseCase,
            AdminRequestGuard adminRequestGuard
    ) {
        this.uploadCourseThumbnailUseCase = uploadCourseThumbnailUseCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a course cover image and return its public URL")
    public ResponseEntity<ApiResponse<CourseThumbnailResponse>> upload(
            @RequestParam("file") MultipartFile file
    ) {
        adminRequestGuard.requireAdmin(AuthenticationContext.requireUser().id());
        final String url = uploadCourseThumbnailUseCase.execute(
                openStream(file), file.getSize(), file.getContentType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new CourseThumbnailResponse(url)));
    }

    private InputStream openStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
