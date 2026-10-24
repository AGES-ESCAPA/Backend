package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.entity.Order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de pedidos em memória. Guarda e devolve cópias, como um banco faria:
 * mudar o pedido lido não altera o que está salvo até {@link #save(Order)}.
 */
public final class InMemoryOrderRepositoryPort implements OrderRepositoryPort {

    private final Map<UUID, Order> orders = new LinkedHashMap<>();
    private int saveCount;

    @Override
    public Order save(Order order) {
        saveCount++;
        if (order.getId() == null) {
            order.setId(UUID.randomUUID());
        }
        orders.put(order.getId(), copyOf(order));
        return copyOf(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id)).map(InMemoryOrderRepositoryPort::copyOf);
    }

    @Override
    public Optional<Order> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    @Override
    public List<Order> findByBuyerUserId(UUID buyerUserId) {
        return orders.values().stream()
                .filter(order -> buyerUserId.equals(order.getBuyerUserId()))
                .map(InMemoryOrderRepositoryPort::copyOf)
                .toList();
    }

    public int saveCount() {
        return saveCount;
    }

    private static Order copyOf(Order source) {
        final Order copy = new Order();
        copy.setId(source.getId());
        copy.setBuyerUserId(source.getBuyerUserId());
        copy.setCompanyId(source.getCompanyId());
        copy.setType(source.getType());
        copy.setItemType(source.getItemType());
        copy.setCourseId(source.getCourseId());
        copy.setSeatsQuantity(source.getSeatsQuantity());
        copy.setAmount(source.getAmount());
        copy.setPaymentMethod(source.getPaymentMethod());
        copy.setInstallments(source.getInstallments());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setStatusHistory(new ArrayList<>(source.getStatusHistory()));
        return copy;
    }
}
