package com.escapa.backend.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderStats;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderStatus;

/**
 * Repositório de pedidos em memória. Guarda e devolve cópias, como um banco faria:
 * mudar o pedido lido não altera o que está salvo até {@link #save(Order)}.
 */
public final class InMemoryOrderRepositoryPort implements OrderRepositoryPort {

    private final Map<UUID, Order> orders = new LinkedHashMap<>();
    private final List<OrderSummary> summaries = new ArrayList<>();
    private final Map<UUID, OrderDetails> details = new LinkedHashMap<>();
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

    @Override
    public PageResult<OrderSummary> findOrders(OrderFilter filter, int page, int size) {
        final List<OrderSummary> filtered = filter(filter).stream()
                .sorted(Comparator.comparing(OrderSummary::purchaseDate).reversed()
                        .thenComparing(OrderSummary::id, Comparator.reverseOrder()))
                .toList();
        final int start = Math.min(page * size, filtered.size());
        final int end = Math.min(start + size, filtered.size());
        final int totalPages = size == 0 ? 0 : (int) Math.ceil((double) filtered.size() / size);
        return new PageResult<>(filtered.subList(start, end), page, size, filtered.size(), totalPages);
    }

    @Override
    public OrderStats summarizeOrders(OrderFilter filter) {
        final List<OrderSummary> filtered = filter(filter);
        final List<OrderSummary> approved = filtered.stream()
                .filter(summary -> summary.status() == OrderStatus.APPROVED)
                .toList();
        final BigDecimal revenue = approved.stream()
                .map(OrderSummary::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderStats(approved.size(), revenue, filtered.size() - approved.size());
    }

    private List<OrderSummary> filter(OrderFilter filter) {
        return summaries.stream()
                .filter(summary -> filter.type() == null || filter.type() == summary.type())
                .filter(summary -> filter.courseId() == null || filter.courseId().equals(summary.courseId()))
                .filter(summary -> filter.status() == null || filter.status() == summary.status())
                .filter(summary -> filter.from() == null
                        || !summary.purchaseDate().toLocalDate().isBefore(filter.from()))
                .filter(summary -> filter.to() == null
                        || !summary.purchaseDate().toLocalDate().isAfter(filter.to()))
                .filter(summary -> filter.search() == null || matchesSearch(summary, filter.search()))
                .toList();
    }

    private static boolean matchesSearch(OrderSummary summary, String search) {
        final String term = search.toLowerCase(Locale.ROOT);
        final String hex = summary.id().toString().replace("-", "");
        final String code = "ped-" + hex.substring(hex.length() - 8);
        return Stream.of(code, summary.requester(), summary.courseTitle())
                .anyMatch(field -> field != null && field.toLowerCase(Locale.ROOT).contains(term));
    }

    @Override
    public Optional<OrderDetails> findDetailsById(UUID id) {
        return Optional.ofNullable(details.get(id));
    }

    public void addSummary(OrderSummary summary) {
        summaries.add(summary);
    }

    public void addDetails(OrderDetails orderDetails) {
        details.put(orderDetails.id(), orderDetails);
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
