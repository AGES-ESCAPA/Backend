package com.escapa.backend.domain.company;

import java.util.UUID;

/**
 * A empresa do token não tem registro em {@code company}. Acontece com um usuário COMPANY criado só
 * em {@code users} (por exemplo, por {@code POST /api/v1/users}), que mesmo assim consegue entrar.
 */
public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException(UUID companyId) {
        super("Company not found: " + companyId);
    }
}
