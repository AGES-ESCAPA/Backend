package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.order.OrderNotFoundException;

import java.util.UUID;

public class GetAdminOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public GetAdminOrderUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public OrderDetails execute(UUID id) {
        return orderRepositoryPort.findDetailsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}

