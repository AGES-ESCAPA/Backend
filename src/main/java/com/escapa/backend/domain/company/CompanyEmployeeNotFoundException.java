package com.escapa.backend.domain.company;

import java.util.UUID;

/** O colaborador não existe ou não está vinculado à empresa; as duas situações são indistinguíveis de fora. */
public class CompanyEmployeeNotFoundException extends RuntimeException {

    public CompanyEmployeeNotFoundException(UUID companyId, UUID employeeId) {
        super("Employee " + employeeId + " not found in company " + companyId);
    }
}
