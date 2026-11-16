package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.model.OrderStats;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;

import java.time.LocalDate;
import java.util.UUID;

/** Totais da gestão de pedidos com os mesmos filtros da listagem, independentes da página exibida. */
public class GetAdminOrderStatsUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public GetAdminOrderStatsUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public OrderStats execute(
            OrderType type,
            UUID courseId,
            LocalDate from,
            LocalDate to,
            OrderStatus status,
            String search
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The 'from' date cannot be after the 'to' date");
        }

        return orderRepositoryPort.summarizeOrders(new OrderFilter(type, courseId, from, to, status, search));
    }
}
