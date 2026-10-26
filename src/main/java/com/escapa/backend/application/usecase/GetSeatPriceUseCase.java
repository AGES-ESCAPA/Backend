package com.escapa.backend.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor de um assento corporativo, para o checkout mostrar o total antes da compra (US-26).
 * É o mesmo valor que o {@link CreateOrderUseCase} usa para calcular o pedido de assentos.
 */
public class GetSeatPriceUseCase {

    private final BigDecimal seatPrice;

    public GetSeatPriceUseCase(BigDecimal seatPrice) {
        this.seatPrice = seatPrice;
    }

    public BigDecimal execute() {
        return seatPrice.setScale(2, RoundingMode.HALF_UP);
    }
}
