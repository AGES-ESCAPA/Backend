package com.escapa.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada do cadastro de usuário. Toda validação de formato mora aqui, via Bean Validation;
 * o service só cuida da regra que depende do banco (email já usado).
 */
public record CreateUserRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email,
        @NotBlank(message = "Password is required")
        @Size(min = MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String password,
        @NotBlank(message = "User type is required") String userType
) {
    public static final int MIN_PASSWORD_LENGTH = 8;
}
