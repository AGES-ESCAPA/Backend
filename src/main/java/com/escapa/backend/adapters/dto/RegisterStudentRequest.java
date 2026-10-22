package com.escapa.backend.adapters.dto;

import java.util.Locale;

import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.RegisterStudentCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Student registration payload")
public record RegisterStudentRequest(
        @NotBlank(message = "Name is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "name " + FieldLimits.VARCHAR_MESSAGE)
        String name,
        @NotBlank(message = "CPF is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "cpf " + FieldLimits.VARCHAR_MESSAGE)
        String cpf,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "email " + FieldLimits.VARCHAR_MESSAGE)
        String email,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "phone " + FieldLimits.VARCHAR_MESSAGE)
        @Schema(description = "Optional Brazilian phone. Masks are accepted and only digits are stored.",
                example = "(54) 99999-8888")
        String phone,
        @NotBlank(message = "Password is required")
        @Size(
                min = CreateUserUseCase.MIN_PASSWORD_LENGTH,
                message = "Password must be at least " + CreateUserUseCase.MIN_PASSWORD_LENGTH + " characters"
        )
        String password
) {

    public RegisterStudentRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public RegisterStudentCommand toCommand() {
        return new RegisterStudentCommand(name, cpf, email, phone, password);
    }
}
