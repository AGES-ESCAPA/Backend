package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.usecase.CreateUserUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInvitePasswordRequest(
        @NotBlank(message = "Password is required")
        @Size(min = CreateUserUseCase.MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String password
) {
}
