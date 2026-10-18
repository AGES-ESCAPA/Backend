package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Representa o corpo recebido pela API durante a atualização do perfil.
public record ProfileUpdateDTO(
    @NotBlank(message = "Nome é obrigatório")
    String name,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email
) {}
