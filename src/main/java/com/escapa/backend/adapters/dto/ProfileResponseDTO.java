package com.escapa.backend.adapters.dto;

// Representa os dados enviados pela API ao consultar ou atualizar um perfil.
public record ProfileResponseDTO(
    // Tipo do usuário, por exemplo STUDENT, COMPANY ou EMPLOYEE.
    String profile,
    // Indica se o usuário pode editar os próprios dados.
    Boolean editable,
    // Dados básicos atualmente utilizados pelo perfil.
    String name,
    String email,
    // Campos reservados para uma futura extensão do perfil.
    String cpf,
    String phone,
    String companyName,
    String responsibleName,
    String corporateName,
    String cnpj
) {

}
