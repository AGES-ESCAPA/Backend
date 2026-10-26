package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.domain.entity.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de pedidos (US-26 / US-28). O pedido é salvo e lido junto com o histórico
 * de mudanças de status.
 */
public interface OrderRepositoryPort {

    /** Cria ou atualiza o pedido; mudanças de status novas entram no histórico. */
    Order save(Order order);

    Optional<Order> findById(UUID id);

    /**
     * Lê o pedido travando-o até o fim da transação, para que duas mudanças de status
     * simultâneas (ex.: cancelar duas vezes) não passem juntas pela mesma validação.
     */
    Optional<Order> findByIdForUpdate(UUID id);

    /** Pedidos do comprador, do mais recente para o mais antigo. */
    List<Order> findByBuyerUserId(UUID buyerUserId);

    /** Pedidos que atendem aos filtros, paginados, do mais recente para o mais antigo. */
    PageResult<OrderSummary> findOrders(OrderFilter filter, int page, int size);

    /** Detalhe do pedido com comprador, item e histórico de status em ordem cronológica. */
    Optional<OrderDetails> findDetailsById(UUID id);
}
