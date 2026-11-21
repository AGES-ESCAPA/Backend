package com.escapa.backend.adapters.dto;

import java.math.BigDecimal;

import com.escapa.backend.application.model.OrderStats;

public record AdminOrderStatsResponse(
        long approvedCount,
        BigDecimal approvedRevenue,
        long cancelledOrRefundedCount
) {

    public static AdminOrderStatsResponse from(OrderStats stats) {
        return new AdminOrderStatsResponse(
                stats.approvedCount(),
                stats.approvedRevenue(),
                stats.cancelledOrRefundedCount()
        );
    }
}
