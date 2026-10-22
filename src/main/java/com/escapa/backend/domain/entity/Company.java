package com.escapa.backend.domain.entity;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados próprios da empresa. O id é o mesmo do {@link User} correspondente
 * (a empresa é um usuário com role COMPANY).
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Company {
    private UUID id;
    private String companyName;
    private String cnpjId;
    private String companyEmail;
    private String phone;

    public Company(UUID id, String companyName, String cnpjId, String companyEmail, String phone) {
        this.id = id;
        this.companyName = companyName;
        this.cnpjId = cnpjId;
        this.companyEmail = companyEmail;
        this.phone = phone;
    }
}