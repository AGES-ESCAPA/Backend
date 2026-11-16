package com.escapa.backend.application.model;

import java.math.BigDecimal;

/**
 * Totais dos pedidos que atendem aos filtros da gestão de pedidos, sobre todas as páginas.
 *
 * @param approvedRevenue soma dos valores dos pedidos aprovados
 */
public record OrderStats(
        long approvedCount,
        BigDecimal approvedRevenue,
        long cancelledOrRefundedCount
) {
}
