package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.usecase.CreateUserUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Company registration payload")
public record RegisterCompanyRequest(
        @NotBlank(message = "responsibleName is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "responsibleName " + FieldLimits.VARCHAR_MESSAGE)
        String responsibleName,
        @NotBlank(message = "cnpj is required")
        String cnpj,
        @NotBlank(message = "companyName is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "companyName " + FieldLimits.VARCHAR_MESSAGE)
        String companyName,
        @NotBlank(message = "email is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "email " + FieldLimits.VARCHAR_MESSAGE)
        String email,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "phone " + FieldLimits.VARCHAR_MESSAGE)
        String phone,
        @NotBlank(message = "password is required")
        @Size(min = CreateUserUseCase.MIN_PASSWORD_LENGTH,
                message = "password must have at least " + CreateUserUseCase.MIN_PASSWORD_LENGTH + " characters")
        String password
) {
}