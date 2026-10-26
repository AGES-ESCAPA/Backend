package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Price of one corporate seat, used to show the total before the purchase")
public record SeatPriceResponse(
        @Schema(description = "Price of one seat", example = "79.00")
        BigDecimal seatPrice
) {

    public static SeatPriceResponse from(BigDecimal seatPrice) {
        return new SeatPriceResponse(seatPrice);
    }
}
