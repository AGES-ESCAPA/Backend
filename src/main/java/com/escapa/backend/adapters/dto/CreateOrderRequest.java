package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Corpo de {@code POST /api/v1/orders}. Não tem valor nem dados de cartão: o valor é calculado no
 * servidor e o corpo só informa o que comprar, a forma de pagamento e as parcelas.
 */
@Schema(description = "Purchase of a course or of corporate seats. The amount is calculated by the server.")
public record CreateOrderRequest(
        @Schema(description = "What is being bought", example = "COURSE")
        @NotNull(message = "itemType is required")
        OrderItemType itemType,

        @Schema(description = "Course id; required when itemType is COURSE")
        UUID courseId,

        @Schema(description = "Number of seats; required when itemType is SEATS (companies only)", example = "10")
        @Positive(message = "seatsQuantity must be greater than zero")
        Integer seatsQuantity,

        @Schema(description = "Only CARD is accepted for now", example = "CARD")
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        @Schema(description = "Number of installments; one when omitted", example = "3")
        @Positive(message = "installments must be at least 1")
        Integer installments
) {
}
