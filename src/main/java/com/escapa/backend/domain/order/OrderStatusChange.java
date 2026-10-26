package com.escapa.backend.domain.order;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma mudança de status do pedido, para o histórico exibido no detalhe do pedido.
 *
 * @param id        nulo enquanto a mudança ainda não foi persistida
 * @param changedBy usuário que fez a mudança, ou {@code null} quando automática
 *                  ou quando o usuário foi removido
 * @param reason    motivo informado, opcional
 */
public record OrderStatusChange(
        UUID id,
        OrderStatus status,
        UUID changedBy,
        String reason,
        LocalDateTime createdAt
) {
}
