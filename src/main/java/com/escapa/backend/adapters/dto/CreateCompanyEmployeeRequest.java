package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data to register an employee of the company")
public record CreateCompanyEmployeeRequest(
        @Schema(example = "João da Silva")
        String name,
        @Schema(example = "joao.silva@empresa.com.br")
        String email
) {
}
