package com.escapa.backend.application.model;

import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.PaymentMethod;

import java.util.UUID;

/**
 * Pedido de compra pedido pelo usuário autenticado. Não tem valor nem dados de cartão: o valor é
 * calculado no servidor e o cartão nunca chega à API (só a forma de pagamento e as parcelas).
 *
 * @param buyerId       id do usuário autenticado; para uma empresa, é o id da própria empresa
 * @param buyerRole     perfil do usuário autenticado (STUDENT ou COMPANY)
 * @param courseId      obrigatório em compra de curso
 * @param seatsQuantity obrigatório em compra de assentos
 * @param installments  opcional; sem valor, vale uma parcela
 */
public record CreateOrderCommand(
        UUID buyerId,
        String buyerRole,
        OrderItemType itemType,
        UUID courseId,
        Integer seatsQuantity,
        PaymentMethod paymentMethod,
        Integer installments
) {
}
