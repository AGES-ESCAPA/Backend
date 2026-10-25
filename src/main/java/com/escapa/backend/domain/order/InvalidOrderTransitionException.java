package com.escapa.backend.domain.order;

import java.util.UUID;

/** Mudança de status que o fluxo APPROVED -> CANCELLED -> REFUNDED não permite. */
public class InvalidOrderTransitionException extends RuntimeException {

    public InvalidOrderTransitionException(UUID orderId, OrderStatus current, OrderStatus target) {
        super("Order " + orderId + " is " + current + " and cannot become " + target);
    }
}
