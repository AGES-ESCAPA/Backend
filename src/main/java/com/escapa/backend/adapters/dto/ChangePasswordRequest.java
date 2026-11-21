package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.escapa.backend.application.usecase.CreateUserUseCase;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = CreateUserUseCase.MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String newPassword
) {
}
