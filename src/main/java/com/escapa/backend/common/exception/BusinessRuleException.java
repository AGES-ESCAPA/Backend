package com.escapa.backend.common.exception;

/**
 * Pedido bem formado, mas uma regra de negócio não permite executá-lo
 * (ex.: instrutor não é admin, pré-requisito forma ciclo). Vira HTTP 422.
 */
public class BusinessRuleException extends BusinessException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
