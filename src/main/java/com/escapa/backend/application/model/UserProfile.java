package com.escapa.backend.application.model;

public record UserProfile(
        String profile,
        boolean editable,
        String name,
        String email,
        String cpf,
        String phone,
        String companyName,
        String cnpj
) {
}
