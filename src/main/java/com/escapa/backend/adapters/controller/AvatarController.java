package com.escapa.backend.adapters.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.AvatarResponse;
import com.escapa.backend.adapters.security.UserIdHeader;
import com.escapa.backend.application.usecase.UploadAvatarUseCase;
import com.escapa.backend.domain.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Me - Avatar", description = "Profile photo of the student identified by X-User-Id")
@RequestMapping("/api/v1/me")
public class AvatarController {

    private final UploadAvatarUseCase uploadAvatarUseCase;

    public AvatarController(UploadAvatarUseCase uploadAvatarUseCase) {
        this.uploadAvatarUseCase = uploadAvatarUseCase;
    }

    @PutMapping("/avatar")
    @Operation(summary = "Upload or replace the profile photo of the logged-in user")
    public ResponseEntity<ApiResponse<AvatarResponse>> uploadAvatar(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @RequestParam("file") MultipartFile file
    ) {
        final UUID userId = UserIdHeader.requireStudentId(xUserId);
        final User updatedUser = uploadAvatarUseCase.execute(
                userId, openStream(file), file.getSize(), file.getContentType());
        return ResponseEntity.ok(ApiResponse.success(new AvatarResponse(updatedUser.getAvatarUrl())));
    }

    private InputStream openStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}