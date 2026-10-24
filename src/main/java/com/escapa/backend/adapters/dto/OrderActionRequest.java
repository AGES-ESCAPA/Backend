package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Corpo opcional de cancelamento e reembolso de pedido: o motivo fica no histórico de status. */
@Schema(description = "Optional body of the cancel and refund actions")
public record OrderActionRequest(
        @Size(max = FieldLimits.VARCHAR_MAX, message = "reason " + FieldLimits.VARCHAR_MESSAGE)
        String reason
) {
}
