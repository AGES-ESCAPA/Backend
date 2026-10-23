package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAccountRequest(
        @NotBlank(message = "Current password is required") String currentPassword
) {
}