package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.PaymentMethod;
import com.escapa.backend.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    interface OrderListingView {
        UUID getId();

        UUID getBuyerUserId();

        String getBuyerName();

        UUID getCompanyId();

        String getCompanyName();

        OrderType getType();

        UUID getCourseId();

        String getCourseTitle();

        Integer getSeatsQuantity();

        LocalDateTime getCreatedAt();

        PaymentMethod getPaymentMethod();

        BigDecimal getAmount();

        OrderStatus getStatus();
    }

    @Override
    @EntityGraph(attributePaths = "statusHistory")
    Optional<OrderEntity> findById(UUID id);

    @EntityGraph(attributePaths = "statusHistory")
    List<OrderEntity> findByBuyerUserIdOrderByCreatedAtDesc(UUID buyerUserId);

    @Query(value = """
            SELECT
                o.id AS id,
                o.buyerUserId AS buyerUserId,
                u.name AS buyerName,
                o.companyId AS companyId,
                comp.companyName AS companyName,
                o.type AS type,
                o.courseId AS courseId,
                c.title AS courseTitle,
                o.seatsQuantity AS seatsQuantity,
                o.createdAt AS createdAt,
                o.paymentMethod AS paymentMethod,
                o.amount AS amount,
                o.status AS status
            FROM OrderEntity o
            LEFT JOIN UserEntity u ON o.buyerUserId = u.id
            LEFT JOIN CompanyEntity comp ON o.companyId = comp.id
            LEFT JOIN CourseEntity c ON o.courseId = c.id
            WHERE (:type IS NULL OR o.type = :type)
              AND (:courseId IS NULL OR o.courseId = :courseId)
              AND (:status IS NULL OR o.status = :status)
              AND (CAST(:fromDateTime AS LocalDateTime) IS NULL OR o.createdAt >= :fromDateTime)
              AND (CAST(:toDateTime AS LocalDateTime) IS NULL OR o.createdAt <= :toDateTime)
            ORDER BY o.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(o)
            FROM OrderEntity o
            WHERE (:type IS NULL OR o.type = :type)
              AND (:courseId IS NULL OR o.courseId = :courseId)
              AND (:status IS NULL OR o.status = :status)
              AND (CAST(:fromDateTime AS LocalDateTime) IS NULL OR o.createdAt >= :fromDateTime)
              AND (CAST(:toDateTime AS LocalDateTime) IS NULL OR o.createdAt <= :toDateTime)
            """)
    Page<OrderListingView> findOrders(
            @Param("type") OrderType type,
            @Param("courseId") UUID courseId,
            @Param("status") OrderStatus status,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(DISTINCT uc)
            FROM UserCourseEntity uc, UsersCompanyEntity comp
            WHERE uc.id.userId = comp.id.userId
              AND comp.id.companyId = :companyId
              AND uc.id.courseId = :courseId
            """)
    int countCollaboratorsWithAccess(
            @Param("companyId") UUID companyId,
            @Param("courseId") UUID courseId
    );
}
