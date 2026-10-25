package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Editable profile fields. Non-editable fields (email, cpf, cnpj) sent in the body are ignored.")
// Representa o corpo recebido pela API durante a atualização do perfil.
public record ProfileUpdateRequest(
        @NotBlank(message = "name is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "name " + FieldLimits.VARCHAR_MESSAGE)
        @Schema(description = "Student name, or the responsible person of a company")
        String name,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "phone " + FieldLimits.VARCHAR_MESSAGE)
        @Schema(description = "Optional Brazilian phone with 10 or 11 digits. Masks are accepted and only "
                + "digits are stored; omitting it clears the phone.", example = "(54) 99999-8888")
        String phone,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "companyName " + FieldLimits.VARCHAR_MESSAGE)
        @Schema(description = "Legal name of the company. Required for COMPANY, ignored for STUDENT.")
        String companyName
) {
}
