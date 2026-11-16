package com.escapa.backend.domain.user;

import java.util.UUID;

/** O convite só é reenviado a quem ainda não criou a senha (status PENDING). */
public class EmployeeNotPendingException extends RuntimeException {

    public EmployeeNotPendingException(UUID employeeId) {
        super("Employee " + employeeId + " is not pending: the invite can only be resent while the password is not created");
    }
}
