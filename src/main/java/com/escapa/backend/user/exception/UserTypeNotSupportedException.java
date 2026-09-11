package com.escapa.backend.user.exception;

import com.escapa.backend.common.exception.BusinessRuleException;
import com.escapa.backend.user.entity.UserRole;

/**
 * O papel é válido no vocabulário, mas este endpoint ainda não sabe criá-lo.
 * Hoje vale para {@code COMPANY}: empresa exige razão social e CNPJ, que virão
 * com a US de cadastro de empresa.
 */
public class UserTypeNotSupportedException extends BusinessRuleException {

    public static final String CODE = "USER_TYPE_NOT_SUPPORTED";

    public UserTypeNotSupportedException(UserRole role) {
        super(CODE, "User type not supported by this endpoint yet: " + role);
    }
}
