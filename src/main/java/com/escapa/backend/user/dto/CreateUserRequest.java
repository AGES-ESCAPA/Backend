package com.escapa.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada do cadastro de usuário. Toda validação de formato mora aqui, via Bean Validation;
 * o service só cuida das regras que dependem do banco ou do negócio (email já usado,
 * papel ainda não suportado).
 *
 * <p>{@code userType} continua texto no JSON, sem diferenciar caixa, por contrato com o frontend.
 * O vocabulário aceito é o de {@code UserRole}.
 */
public record CreateUserRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email,
        @NotBlank(message = "Password is required")
        @Size(min = MIN_PASSWORD_LENGTH, message = "Password must be at least 8 characters")
        String password,
        @NotBlank(message = "User type is required")
        @Pattern(regexp = "(?i)\\s*(STUDENT|ADMIN|COMPANY)\\s*",
                message = "User type must be STUDENT, ADMIN or COMPANY")
        String userType
) {
    public static final int MIN_PASSWORD_LENGTH = 8;
}
