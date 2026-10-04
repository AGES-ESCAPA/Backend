package com.escapa.backend.adapters.dto;

// Representa o corpo recebido pela API durante a atualização do perfil.
public record ProfileUpdateDTO(
    // Mantidos no contrato para compatibilidade com o formato planejado do perfil.
    String profile,
    Boolean editable,
    // O serviço utiliza estes campos para atualizar os dados básicos do perfil.
    String name,
    String email,
    // Campos reservados para futuras versões do perfil.
    String cpf,
    String phone,
    String companyName,
    String responsibleName,
    String corporateName,
    String cnpj
) {}
