package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;

import java.time.LocalDate;
import java.util.UUID;

public class ListAdminOrdersUseCase {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepositoryPort orderRepositoryPort;

    public ListAdminOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public PageResult<OrderSummary> execute(
            OrderType type,
            UUID courseId,
            LocalDate from,
            LocalDate to,
            OrderStatus status,
            int page,
            int size
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The 'from' date cannot be after the 'to' date");
        }

        final int safePage = Math.max(page, 0);
        final int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        final OrderFilter filter = new OrderFilter(type, courseId, from, to, status);
        return orderRepositoryPort.findOrders(filter, safePage, safeSize);
    }
}
