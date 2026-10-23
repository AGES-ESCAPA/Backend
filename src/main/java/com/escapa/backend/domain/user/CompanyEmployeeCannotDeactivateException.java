package com.escapa.backend.domain.user;

import java.util.UUID;

public class CompanyEmployeeCannotDeactivateException extends RuntimeException {

    public CompanyEmployeeCannotDeactivateException(UUID userId) {
        super("Company employees cannot deactivate their own account: " + userId);
    }
}