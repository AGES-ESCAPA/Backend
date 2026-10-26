package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepositoryPort {

    boolean existsByCnpjId(String cnpjId);

    /**
     * Grava o usuário (tabela users) e os dados da empresa (tabela company) juntos.
     * O id de {@code user} e de {@code company} deve ser o mesmo.
     */
    Company save(User user, Company company);

    Optional<Company> findById(UUID id);

    void update(Company company);
}