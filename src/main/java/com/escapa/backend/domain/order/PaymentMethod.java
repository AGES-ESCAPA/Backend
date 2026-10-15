package com.escapa.backend.domain.order;

/**
 * Formas de pagamento previstas no banco. Hoje só {@link #CARD} existe no fluxo;
 * PIX e BOLETO ficam previstos para quando houver um meio de pagamento real, e é
 * a API de criação de pedido que recusa os valores ainda não usados.
 */
public enum PaymentMethod {
    CARD,
    PIX,
    BOLETO
}
