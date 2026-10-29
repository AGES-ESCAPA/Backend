package com.escapa.backend.infrastructure.persistence;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;

import com.escapa.backend.application.port.RegularUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;

import jakarta.persistence.EntityManager;

public class RegularUserRepositoryAdapter implements RegularUserRepositoryPort {

    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
    private static final String EMAIL_CONSTRAINT = "users_email_key";
    private static final String CPF_CONSTRAINT = "regular_users_cpf_key";

    private final RegularUserJpaRepository regularUserJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final EntityManager entityManager;

    public RegularUserRepositoryAdapter(
            RegularUserJpaRepository regularUserJpaRepository,
            UserJpaRepository userJpaRepository,
            EntityManager entityManager
    ) {
        this.regularUserJpaRepository = regularUserJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public User save(User user, String cpf, String phone) {
        final UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        final LocalDateTime createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt()
                : LocalDateTime.now();
        final RegularUserEntity entity = new RegularUserEntity(
                id,
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getUserType(),
                createdAt,
                cpf,
                phone
        );
        lockRegistration(user.getEmail(), cpf);
        rejectIfDuplicate(user.getEmail(), cpf);
        try {
            return UserMapper.toDomain(regularUserJpaRepository.saveAndFlush(entity));
        } catch (RuntimeException exception) {
            throw translateConflict(exception);
        }
    }

    private void lockRegistration(String email, String cpf) {
        advisoryLock("email", email);
        advisoryLock("cpf", cpf);
    }

    private void advisoryLock(String namespace, String value) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:namespace), hashtext(:value))")
                .setParameter("namespace", namespace)
                .setParameter("value", value)
                .getResultList();
    }

    private void rejectIfDuplicate(String email, String cpf) {
        if (userJpaRepository.existsByEmail(email)) {
            throw new DuplicateRegistrationException("email");
        }
        if (regularUserJpaRepository.existsByCpf(cpf)) {
            throw new DuplicateRegistrationException("cpf");
        }
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return regularUserJpaRepository.existsByCpf(cpf);
    }

    public static RuntimeException translateConflict(RuntimeException exception) {
        final String constraint = uniqueConstraintName(exception);
        if (EMAIL_CONSTRAINT.equals(constraint)) {
            return new DuplicateRegistrationException("email");
        }
        if (CPF_CONSTRAINT.equals(constraint)) {
            return new DuplicateRegistrationException("cpf");
        }
        return exception;
    }

    private static String uniqueConstraintName(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        final String fromCurrent = constraintNameFrom(throwable);
        if (fromCurrent != null) {
            return fromCurrent;
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            final String fromSuppressed = uniqueConstraintName(suppressed);
            if (fromSuppressed != null) {
                return fromSuppressed;
            }
        }
        if (throwable instanceof SQLException sql) {
            final String fromNext = uniqueConstraintName(sql.getNextException());
            if (fromNext != null) {
                return fromNext;
            }
        }
        final Throwable cause = throwable.getCause();
        return cause == throwable ? null : uniqueConstraintName(cause);
    }

    private static String constraintNameFrom(Throwable throwable) {
        if (throwable instanceof ConstraintViolationException violation
                && UNIQUE_VIOLATION_SQL_STATE.equals(violation.getSQLState())) {
            return normalizeConstraint(violation.getConstraintName());
        }
        if (throwable instanceof SQLException sql
                && UNIQUE_VIOLATION_SQL_STATE.equals(sql.getSQLState())) {
            return postgresConstraint(sql);
        }
        return null;
    }

    private static String postgresConstraint(SQLException sql) {
        try {
            final Object serverError = sql.getClass().getMethod("getServerErrorMessage").invoke(sql);
            if (serverError == null) {
                return null;
            }
            return normalizeConstraint(
                    (String) serverError.getClass().getMethod("getConstraint").invoke(serverError));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String normalizeConstraint(String name) {
        if (name == null) {
            return null;
        }
        final String unquoted = name.replace("\"", "");
        final int dot = unquoted.lastIndexOf('.');
        return dot < 0 ? unquoted : unquoted.substring(dot + 1);
    }
}
