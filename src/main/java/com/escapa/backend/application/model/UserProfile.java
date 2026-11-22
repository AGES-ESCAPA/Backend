package com.escapa.backend.application.model;

public record UserProfile(
        String profile,
        boolean editable,
        String name,
        String email,
        String cpf,
        String phone,
        String companyName,
        String cnpj,
        String avatarUrl,
        /** Título curto do instrutor nos cursos; só faz sentido para ADMIN. */
        String headline,
        /** Biografia do instrutor nos cursos; só faz sentido para ADMIN. */
        String bio
) {
}
