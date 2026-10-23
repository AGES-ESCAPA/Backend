package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatusChange;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.infrastructure.persistence.entity.CompanyEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.OrderEntity;
import com.escapa.backend.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository repository;
    private final RegularUserJpaRepository regularUserJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CourseJpaRepository courseJpaRepository;

    public OrderRepositoryAdapter(
            OrderJpaRepository repository,
            RegularUserJpaRepository regularUserJpaRepository,
            CompanyJpaRepository companyJpaRepository,
            UserJpaRepository userJpaRepository,
            CourseJpaRepository courseJpaRepository
    ) {
        this.repository = repository;
        this.regularUserJpaRepository = regularUserJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.courseJpaRepository = courseJpaRepository;
    }

    @Override
    public Order save(Order order) {
        return OrderMapper.toDomain(repository.save(OrderMapper.toEntity(order)));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByBuyerUserId(UUID buyerUserId) {
        return repository.findByBuyerUserIdOrderByCreatedAtDesc(buyerUserId).stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<OrderSummary> findOrders(OrderFilter filter, int page, int size) {
        final Pageable pageable = PageRequest.of(page, size);
        final LocalDateTime fromDateTime = filter.from() != null ? filter.from().atStartOfDay() : null;
        final LocalDateTime toDateTime = filter.to() != null ? filter.to().atTime(LocalTime.MAX) : null;

        final Page<OrderJpaRepository.OrderListingView> jpaPage = repository.findOrders(
                filter.type(), filter.courseId(), filter.status(), fromDateTime, toDateTime, pageable);

        final List<OrderSummary> content = jpaPage.getContent().stream()
                .map(this::toOrderSummary)
                .toList();

        return new PageResult<>(
                content, jpaPage.getNumber(), jpaPage.getSize(),
                jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }

    @Override
    public Optional<OrderDetails> findDetailsById(UUID id) {
        return repository.findById(id).map(entity -> {
            final OrderDetails.Buyer buyer = buildBuyer(entity);
            final OrderDetails.Item item = buildItem(entity);
            final List<OrderStatusChange> statusHistory = toStatusHistory(entity.getStatusHistory());
            final Integer collaboratorsCount = resolveCollaboratorsWithAccess(entity);

            return new OrderDetails(
                    entity.getId(),
                    entity.getType(),
                    buyer,
                    item,
                    entity.getAmount(),
                    entity.getPaymentMethod(),
                    entity.getInstallments(),
                    entity.getStatus(),
                    entity.getCreatedAt(),
                    statusHistory,
                    collaboratorsCount
            );
        });
    }

    private OrderSummary toOrderSummary(OrderJpaRepository.OrderListingView view) {
        final String requester;
        if (view.getType() == OrderType.CORPORATE && view.getCompanyName() != null) {
            requester = view.getCompanyName();
        } else {
            requester = view.getBuyerName();
        }

        return new OrderSummary(
                view.getId(),
                requester,
                view.getType(),
                view.getCourseId(),
                view.getCourseTitle(),
                view.getSeatsQuantity(),
                view.getCreatedAt(),
                view.getPaymentMethod(),
                view.getAmount(),
                view.getStatus()
        );
    }

    private OrderDetails.Buyer buildBuyer(OrderEntity entity) {
        if (entity.getType() == OrderType.CORPORATE && entity.getCompanyId() != null) {
            return companyJpaRepository.findById(entity.getCompanyId())
                    .map(this::toCorporateBuyer)
                    .orElseGet(() -> fallbackBuyer(entity.getBuyerUserId()));
        }

        return regularUserJpaRepository.findById(entity.getBuyerUserId())
                .map(this::toIndividualBuyer)
                .orElseGet(() -> fallbackBuyer(entity.getBuyerUserId()));
    }

    private OrderDetails.Buyer toCorporateBuyer(CompanyEntity company) {
        final String email = company.getCompanyEmail() != null ? company.getCompanyEmail() : company.getEmail();
        return new OrderDetails.Buyer(
                company.getId(), company.getCompanyName(), email, company.getCnpjId(), company.getPhone());
    }

    private OrderDetails.Buyer toIndividualBuyer(RegularUserEntity regularUser) {
        return new OrderDetails.Buyer(
                regularUser.getId(), regularUser.getName(), regularUser.getEmail(),
                regularUser.getCpf(), regularUser.getPhone());
    }

    private OrderDetails.Buyer fallbackBuyer(UUID buyerUserId) {
        return userJpaRepository.findById(buyerUserId)
                .map(user -> new OrderDetails.Buyer(user.getId(), user.getName(), user.getEmail(), null, null))
                .orElse(null);
    }

    private OrderDetails.Item buildItem(OrderEntity entity) {
        if (entity.getItemType() == OrderItemType.COURSE && entity.getCourseId() != null) {
            final String courseTitle = courseJpaRepository.findById(entity.getCourseId())
                    .map(CourseEntity::getTitle)
                    .orElse(null);
            return new OrderDetails.Item(OrderItemType.COURSE, entity.getCourseId(), courseTitle, null);
        }
        return new OrderDetails.Item(OrderItemType.SEATS, null, null, entity.getSeatsQuantity());
    }

    private List<OrderStatusChange> toStatusHistory(List<OrderStatusHistoryEntity> history) {
        return history.stream()
                .map(h -> new OrderStatusChange(h.getId(), h.getStatus(), h.getChangedBy(), h.getReason(), h.getCreatedAt()))
                .toList();
    }

    private Integer resolveCollaboratorsWithAccess(OrderEntity entity) {
        if (entity.getType() == OrderType.CORPORATE
                && entity.getItemType() == OrderItemType.COURSE
                && entity.getCompanyId() != null
                && entity.getCourseId() != null) {
            return repository.countCollaboratorsWithAccess(entity.getCompanyId(), entity.getCourseId());
        }
        return null;
    }
}
