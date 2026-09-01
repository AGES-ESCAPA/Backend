package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email,
        String password,
        @NotBlank(message = "User type is required") String userType
) {
}
