package com.escapa.backend.domain.user;

/** O solicitante nao e um usuario ADMIN valido, exigido pelas rotas administrativas. */
public class AdminAccessDeniedException extends RuntimeException {

    public AdminAccessDeniedException(String message) {
        super(message);
    }
}
