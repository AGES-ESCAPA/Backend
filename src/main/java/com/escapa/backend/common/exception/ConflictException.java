package com.escapa.backend.common.exception;

/** Pedido colide com estado já existente (ex.: email duplicado). Vira HTTP 409. */
public class ConflictException extends BusinessException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
