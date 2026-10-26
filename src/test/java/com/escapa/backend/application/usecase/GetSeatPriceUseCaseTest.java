package com.escapa.backend.application.usecase;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetSeatPriceUseCaseTest {

    @Test
    void shouldReturnTheConfiguredSeatPriceWithTwoDecimals() {
        assertEquals(new BigDecimal("79.00"), new GetSeatPriceUseCase(new BigDecimal("79")).execute());
    }

    @Test
    void shouldRoundTheConfiguredSeatPriceToCents() {
        assertEquals(new BigDecimal("79.99"), new GetSeatPriceUseCase(new BigDecimal("79.985")).execute());
    }
}
