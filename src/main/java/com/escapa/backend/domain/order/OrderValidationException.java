package com.escapa.backend.domain.order;

/** Pedido recusado por regra de negócio: forma de pagamento, combinação de perfil e item, curso não publicado. */
public class OrderValidationException extends RuntimeException {

    public OrderValidationException(String message) {
        super(message);
    }
}
