package com.escapa.backend.common.exception;

/** Recurso pedido não existe. Vira HTTP 404. */
public class NotFoundException extends BusinessException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
