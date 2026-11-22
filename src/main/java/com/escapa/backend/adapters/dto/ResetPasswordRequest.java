package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.escapa.backend.application.usecase.CreateUserUseCase;

public record ResetPasswordRequest(
        @NotBlank(message = "Password is required")
        @Size(min = CreateUserUseCase.MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String password
) {
}
