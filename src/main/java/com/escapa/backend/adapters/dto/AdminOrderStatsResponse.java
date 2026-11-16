package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.OrderStats;

import java.math.BigDecimal;

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
