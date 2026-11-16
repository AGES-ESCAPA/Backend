package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CreateOrderCommand;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.OrderAccessPort;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.CourseAlreadyActiveException;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.OrderValidationException;
import com.escapa.backend.domain.order.PaymentMethod;
import com.escapa.backend.domain.user.UserTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Registra a compra simulada como aprovada e libera o acesso na mesma hora (US-27).
 *
 * <p>O valor é sempre calculado aqui, nunca recebido do cliente: o preço do curso, ou a quantidade
 * de assentos vezes o valor do assento (configuração). O pedido e a liberação de acesso rodam na
 * mesma transação, então uma falha em qualquer etapa desfaz as duas coisas.
 *
 * <p>Na compra de curso por empresa, todos os colaboradores já vinculados são matriculados na mesma
 * transação (US-31, #87), com as mesmas regras do cadastro de colaborador: quem já tem matrícula ativa
 * no curso fica como está, e uma matrícula revogada ou vencida é reativada mantendo o progresso.
 *
 * <p>Só o cartão é aceito por enquanto: PIX e BOLETO existem na coluna, mas aprovar na hora um
 * pedido marcado assim registraria um pagamento que nunca aconteceu.
 */
public class CreateOrderUseCase {

    private static final String APPROVAL_REASON = "Compra simulada aprovada automaticamente";

    private final OrderRepositoryPort orderRepositoryPort;
    private final CourseRepositoryPort courseRepositoryPort;
    private final OrderAccessPort orderAccessPort;
    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;
    private final BigDecimal seatPrice;
    private final Clock clock;

    public CreateOrderUseCase(
            OrderRepositoryPort orderRepositoryPort,
            CourseRepositoryPort courseRepositoryPort,
            OrderAccessPort orderAccessPort,
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort,
            BigDecimal seatPrice,
            Clock clock
    ) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.courseRepositoryPort = courseRepositoryPort;
        this.orderAccessPort = orderAccessPort;
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
        this.seatPrice = seatPrice;
        this.clock = clock;
    }

    public Order execute(CreateOrderCommand command) {
        validateBuyerAndPayment(command);

        if (command.itemType() == OrderItemType.SEATS) {
            return buySeats(command);
        }
        return buyCourse(command);
    }

    private void validateBuyerAndPayment(CreateOrderCommand command) {
        if (command.itemType() == null) {
            throw new OrderValidationException("itemType is required");
        }
        if (!isStudent(command) && !isCompany(command)) {
            throw new OrderValidationException("Only students and companies can place orders");
        }
        if (command.paymentMethod() != PaymentMethod.CARD) {
            throw new OrderValidationException("Only CARD is accepted as payment method for now");
        }
        if (command.installments() != null && command.installments() < 1) {
            throw new OrderValidationException("installments must be at least 1");
        }
    }

    private Order buyCourse(CreateOrderCommand command) {
        final Course course = findPublishedCourse(command.courseId());
        final LocalDate today = LocalDate.now(clock);
        final LocalDate expirationDate = course.getDeadline() == null ? null : today.plusDays(course.getDeadline());

        if (isCompany(command)) {
            requireNoActiveAccess(orderAccessPort.findCompanyCourse(command.buyerId(), course.getId()), today);
            final Order order = orderRepositoryPort.save(approvedOrder(command, courseAmount(course), course.getId(), null));
            // O prazo do curso conta a partir da matrícula de cada colaborador, não da compra da empresa.
            orderAccessPort.saveCompanyCourse(command.buyerId(), course.getId(), today, null);
            enrollCompanyEmployees(command.buyerId(), course.getId(), today, expirationDate);
            return order;
        }

        requireNoActiveAccess(orderAccessPort.findStudentEnrollment(command.buyerId(), course.getId()), today);
        final Order order = orderRepositoryPort.save(approvedOrder(command, courseAmount(course), course.getId(), null));
        orderAccessPort.saveStudentEnrollment(command.buyerId(), course.getId(), today, expirationDate);
        return order;
    }

    private Order buySeats(CreateOrderCommand command) {
        if (!isCompany(command)) {
            throw new OrderValidationException("Only companies can buy seats");
        }
        final Integer quantity = command.seatsQuantity();
        if (quantity == null || quantity < 1) {
            throw new OrderValidationException("seatsQuantity must be greater than zero");
        }

        final BigDecimal amount = seatPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        final Order order = orderRepositoryPort.save(approvedOrder(command, amount, null, quantity));
        orderAccessPort.addCompanySeats(command.buyerId(), quantity);
        return order;
    }

    /**
     * Matricula cada colaborador vinculado, de qualquer status: o status da conta é que controla a
     * entrada na plataforma, então quem for reativado já encontra o curso. Uma matrícula ativa (por
     * exemplo, de uma compra individual) não é duplicada nem tem as datas mudadas.
     */
    private void enrollCompanyEmployees(UUID companyId, UUID courseId, LocalDate today, LocalDate expirationDate) {
        for (final UUID employeeId : companyEmployeeRepositoryPort.findEmployeeIds(companyId)) {
            if (!isActive(orderAccessPort.findStudentEnrollment(employeeId, courseId), today)) {
                orderAccessPort.saveStudentEnrollment(employeeId, courseId, today, expirationDate);
            }
        }
    }

    private Course findPublishedCourse(UUID courseId) {
        if (courseId == null) {
            throw new OrderValidationException("courseId is required to buy a course");
        }
        final Course course = courseRepositoryPort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new OrderValidationException("Only published courses can be purchased");
        }
        return course;
    }

    /** Acesso ativo é o que não tem data de fim ou ainda não venceu; um acesso vencido pode ser renovado. */
    private void requireNoActiveAccess(Optional<Enrollment> access, LocalDate today) {
        if (isActive(access, today)) {
            throw new CourseAlreadyActiveException("The buyer already has active access to this course");
        }
    }

    private static boolean isActive(Optional<Enrollment> access, LocalDate today) {
        return access
                .map(found -> found.expirationDate() == null || !found.expirationDate().isBefore(today))
                .orElse(false);
    }

    private BigDecimal courseAmount(Course course) {
        final double price = course.getPrice() == null ? 0.0 : course.getPrice();
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }

    private Order approvedOrder(CreateOrderCommand command, BigDecimal amount, UUID courseId, Integer seatsQuantity) {
        final boolean company = isCompany(command);
        final Order order = new Order();
        order.setBuyerUserId(command.buyerId());
        order.setCompanyId(company ? command.buyerId() : null);
        order.setType(company ? OrderType.CORPORATE : OrderType.INDIVIDUAL);
        order.setItemType(command.itemType());
        order.setCourseId(courseId);
        order.setSeatsQuantity(seatsQuantity);
        order.setAmount(amount);
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setInstallments(command.installments() == null ? 1 : command.installments());
        order.setCreatedAt(LocalDateTime.now(clock));
        order.changeStatus(OrderStatus.APPROVED, null, APPROVAL_REASON);
        return order;
    }

    private static boolean isStudent(CreateOrderCommand command) {
        return UserTypes.STUDENT.equals(command.buyerRole());
    }

    private static boolean isCompany(CreateOrderCommand command) {
        return UserTypes.COMPANY.equals(command.buyerRole());
    }
}
