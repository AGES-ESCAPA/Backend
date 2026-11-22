package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Novo e-mail solicitado pelo admin; só passa a valer após confirmação no link.")
public record RequestEmailChangeRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "email " + FieldLimits.VARCHAR_MESSAGE)
        String email
) {
}
