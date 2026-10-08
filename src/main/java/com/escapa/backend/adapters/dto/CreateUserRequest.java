package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.usecase.CreateUserUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration payload")
public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "name " + FieldLimits.VARCHAR_MESSAGE)
        String name,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "email " + FieldLimits.VARCHAR_MESSAGE)
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = CreateUserUseCase.MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String password,
        @NotBlank(message = "User type is required") String userType
) {
}
