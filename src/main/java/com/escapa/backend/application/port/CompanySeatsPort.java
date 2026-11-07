package com.escapa.backend.application.port;

import java.util.UUID;

/**
 * Assentos corporativos de uma empresa. O assento em uso é o colaborador vinculado
 * à empresa em {@code users_company}; o banco não guarda atribuição de assento à parte.
 */
public interface CompanySeatsPort {

    /** @throws com.escapa.backend.domain.company.CompanyNotFoundException se a empresa não tem registro em {@code company} */
    int findSeatsTotal(UUID companyId);

    int countSeatsInUse(UUID companyId);

    void updateSeatsTotal(UUID companyId, int seatsTotal);
}
