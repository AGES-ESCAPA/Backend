package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.port.OrderAccessPort;
import com.escapa.backend.domain.order.OrderValidationException;
import com.escapa.backend.infrastructure.persistence.entity.CompanyCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CompanyCourseId;
import com.escapa.backend.infrastructure.persistence.entity.CompanyEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Liberação de acesso de um pedido aprovado (US-27). Usa o {@link EntityManager} da transação do
 * caso de uso, então o pedido e o acesso confirmam ou revertem juntos.
 */
public class OrderAccessAdapter implements OrderAccessPort {

    private final UserCourseJpaRepository userCourseRepository;
    private final EntityManager entityManager;

    public OrderAccessAdapter(UserCourseJpaRepository userCourseRepository, EntityManager entityManager) {
        this.userCourseRepository = userCourseRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Enrollment> findStudentEnrollment(UUID userId, UUID courseId) {
        return userCourseRepository.findById(new UserCourseId(userId, courseId))
                .map(enrollment -> new Enrollment(enrollment.getDtInicio(), enrollment.getDtExpiracao()));
    }

    @Override
    public void saveStudentEnrollment(UUID userId, UUID courseId, LocalDate startDate, LocalDate expirationDate) {
        final UserCourseId id = new UserCourseId(userId, courseId);
        // Uma matrícula que já existe (vencida) só tem as datas renovadas: o progresso é mantido.
        final UserCourseEntity enrollment = userCourseRepository.findById(id).orElseGet(() -> newEnrollment(id));
        enrollment.setDtInicio(startDate);
        enrollment.setDtExpiracao(expirationDate);
        userCourseRepository.save(enrollment);
    }

    @Override
    public Optional<Enrollment> findCompanyCourse(UUID companyId, UUID courseId) {
        return Optional.ofNullable(entityManager.find(CompanyCourseEntity.class, new CompanyCourseId(companyId, courseId)))
                .map(course -> new Enrollment(course.getDataInicio(), course.getDataExpiracao()));
    }

    @Override
    public void saveCompanyCourse(UUID companyId, UUID courseId, LocalDate startDate, LocalDate expirationDate) {
        final CompanyCourseId id = new CompanyCourseId(companyId, courseId);
        final CompanyCourseEntity existing = entityManager.find(CompanyCourseEntity.class, id);
        if (existing != null) {
            existing.setDataInicio(startDate);
            existing.setDataExpiracao(expirationDate);
            return;
        }
        entityManager.persist(new CompanyCourseEntity(
                id,
                entityManager.getReference(CompanyEntity.class, companyId),
                entityManager.getReference(CourseEntity.class, courseId),
                startDate,
                expirationDate));
    }

    @Override
    public void addCompanySeats(UUID companyId, int quantity) {
        // Trava a linha da empresa: duas compras ao mesmo tempo não podem se sobrescrever.
        final CompanyEntity company = entityManager.find(CompanyEntity.class, companyId, LockModeType.PESSIMISTIC_WRITE);
        if (company == null) {
            throw new OrderValidationException("Company not found: " + companyId);
        }
        company.setSeatsTotal(company.getSeatsTotal() + quantity);
    }

    private UserCourseEntity newEnrollment(UserCourseId id) {
        final UserCourseEntity enrollment = new UserCourseEntity();
        enrollment.setId(id);
        enrollment.setUser(entityManager.getReference(UserEntity.class, id.getUserId()));
        enrollment.setCourse(entityManager.getReference(CourseEntity.class, id.getCourseId()));
        enrollment.setProgress(0);
        return enrollment;
    }
}
