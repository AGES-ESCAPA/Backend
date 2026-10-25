package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CompanySeatsPort;
import com.escapa.backend.application.port.OrderAccessRevocationPort;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderNotFoundException;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.SeatsInUseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cancela um pedido aprovado e retira o acesso que ele liberou, sem apagar matrícula,
 * progresso nem certificado. O status só é salvo depois da revogação; o caso de uso roda
 * em uma transação, então uma falha na revogação desfaz tudo.
 */
public class CancelOrderUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderUseCase.class);

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderAccessRevocationPort accessRevocationPort;
    private final CompanySeatsPort companySeatsPort;
    private final Clock clock;

    public CancelOrderUseCase(
            OrderRepositoryPort orderRepositoryPort,
            OrderAccessRevocationPort accessRevocationPort,
            CompanySeatsPort companySeatsPort,
            Clock clock
    ) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.accessRevocationPort = accessRevocationPort;
        this.companySeatsPort = companySeatsPort;
        this.clock = clock;
    }

    public Order execute(UUID orderId, UUID adminId, String reason) {
        final Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel(adminId, reason);

        if (order.getItemType() == OrderItemType.SEATS) {
            releaseSeats(order);
        } else {
            revokeCourseAccess(order);
        }

        final Order saved = orderRepositoryPort.save(order);
        LOGGER.info("Order {} cancelled by admin {}", saved.getId(), adminId);
        return saved;
    }

    private void revokeCourseAccess(Order order) {
        final LocalDateTime now = LocalDateTime.now(clock);
        if (order.getType() == OrderType.CORPORATE) {
            accessRevocationPort.revokeCompanyCourseAccess(order.getCompanyId(), order.getCourseId(), now);
        } else {
            accessRevocationPort.revokeStudentCourseAccess(order.getBuyerUserId(), order.getCourseId(), now);
        }
    }

    /**
     * Subtrai os assentos do pedido do total da empresa, recusando se os colaboradores
     * que já os ocupam ficariam sem assento. O total nunca fica negativo.
     */
    private void releaseSeats(Order order) {
        final UUID companyId = order.getCompanyId();
        final int seatsAfterCancellation =
                Math.max(companySeatsPort.findSeatsTotal(companyId) - order.getSeatsQuantity(), 0);
        final int seatsInUse = companySeatsPort.countSeatsInUse(companyId);
        if (seatsInUse > seatsAfterCancellation) {
            throw new SeatsInUseException(seatsInUse, seatsAfterCancellation);
        }
        companySeatsPort.updateSeatsTotal(companyId, seatsAfterCancellation);
    }
}
