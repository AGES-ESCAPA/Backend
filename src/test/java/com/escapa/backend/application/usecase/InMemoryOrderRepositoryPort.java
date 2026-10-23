package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.entity.Order;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryOrderRepositoryPort implements OrderRepositoryPort {

    private final Map<UUID, Order> orders = new HashMap<>();
    private final List<OrderSummary> summaries = new ArrayList<>();
    private final Map<UUID, OrderDetails> detailsMap = new HashMap<>();

    public void addOrder(Order order) {
        orders.put(order.getId(), order);
    }

    public void addSummary(OrderSummary summary) {
        summaries.add(summary);
    }

    public void addDetails(OrderDetails details) {
        detailsMap.put(details.id(), details);
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(UUID.randomUUID());
        }
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findByBuyerUserId(UUID buyerUserId) {
        return orders.values().stream()
                .filter(o -> buyerUserId.equals(o.getBuyerUserId()))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public PageResult<OrderSummary> findOrders(OrderFilter filter, int page, int size) {
        final LocalDateTime from = filter.from() != null ? filter.from().atStartOfDay() : null;
        final LocalDateTime to = filter.to() != null ? filter.to().atTime(LocalTime.MAX) : null;

        final List<OrderSummary> filtered = summaries.stream()
                .filter(s -> filter.type() == null || s.type() == filter.type())
                .filter(s -> filter.courseId() == null || filter.courseId().equals(s.courseId()))
                .filter(s -> filter.status() == null || s.status() == filter.status())
                .filter(s -> from == null || !s.purchaseDate().isBefore(from))
                .filter(s -> to == null || !s.purchaseDate().isAfter(to))
                .sorted(Comparator.comparing(OrderSummary::purchaseDate).reversed())
                .toList();

        final int fromIndex = Math.min(page * size, filtered.size());
        final int toIndex = Math.min((page + 1) * size, filtered.size());
        final List<OrderSummary> content = filtered.subList(fromIndex, toIndex);
        final int totalPages = size == 0 ? 0 : (int) Math.ceil((double) filtered.size() / size);

        return new PageResult<>(content, page, size, filtered.size(), totalPages);
    }

    @Override
    public Optional<OrderDetails> findDetailsById(UUID id) {
        return Optional.ofNullable(detailsMap.get(id));
    }
}
