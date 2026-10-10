package com.escapa.backend.domain.entity;

import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Compra de um curso ou de assentos corporativos.
 *
 * <p>Não guarda dados de cartão: registra só a forma de pagamento e as parcelas.
 * {@code companyId} só existe em compra corporativa, {@code courseId} só em compra
 * de curso e {@code seatsQuantity} só em compra de assentos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Order {
    private UUID id;
    private UUID buyerUserId;
    private UUID companyId;
    private OrderType type;
    private OrderItemType itemType;
    private UUID courseId;
    private Integer seatsQuantity;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private Integer installments;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderStatusChange> statusHistory = new ArrayList<>();

    /** Muda o status e registra a mudança no histórico do pedido. */
    public void changeStatus(OrderStatus newStatus, UUID changedBy, String reason) {
        this.status = newStatus;
        this.statusHistory.add(new OrderStatusChange(null, newStatus, changedBy, reason, LocalDateTime.now()));
    }
}
