package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;

import com.escapa.backend.domain.registration.DuplicateRegistrationException;

import jakarta.persistence.PersistenceException;

class RegularUserConflictTranslationTest {

    @Test
    void shouldIdentifyEmailConflictEvenWhenTheEmailContainsCpf() {
        final SQLException sqlException = new SQLException(
                "Key (email)=(cpf@exemplo.com) already exists", "23505");
        final ConstraintViolationException violation = new ConstraintViolationException(
                "Insert failed", sqlException, "users_email_key");
        final DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Save failed", new IllegalStateException(violation));

        final RuntimeException translated = RegularUserRepositoryAdapter.translateConflict(exception);

        assertInstanceOf(DuplicateRegistrationException.class, translated);
        assertEquals("email already registered", translated.getMessage());
    }

    @Test
    void shouldIdentifyCpfConflictWithoutAnErrorMessage() {
        final DataIntegrityViolationException exception = violation("23505", "regular_users_cpf_key", null);

        final RuntimeException translated = RegularUserRepositoryAdapter.translateConflict(exception);

        assertInstanceOf(DuplicateRegistrationException.class, translated);
        assertEquals("cpf already registered", translated.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"23502", "23503", "23514", "22001"})
    void shouldPreserveErrorsThatAreNotUniqueViolations(String sqlState) {
        final DataIntegrityViolationException exception = violation(
                sqlState, "users_email_key", "Invalid email or cpf");

        assertSame(exception, RegularUserRepositoryAdapter.translateConflict(exception));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"users_pkey", "users_email_key_other", "regular_users_cpf_key_other"})
    void shouldPreserveUnknownOrMissingConstraints(String constraintName) {
        final DataIntegrityViolationException exception = violation(
                "23505", constraintName, "Email and cpf already exist");

        assertSame(exception, RegularUserRepositoryAdapter.translateConflict(exception));
    }

    @Test
    void shouldNotInferAConstraintFromASqlExceptionMessage() {
        final DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Save failed", new SQLException("users_email_key cpf@exemplo.com", "23505"));

        assertSame(exception, RegularUserRepositoryAdapter.translateConflict(exception));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Email already exists", "CPF already exists"})
    void shouldPreserveErrorsWithoutACause(String message) {
        final DataIntegrityViolationException exception = new DataIntegrityViolationException(message);

        assertSame(exception, RegularUserRepositoryAdapter.translateConflict(exception));
    }

    @ParameterizedTest
    @ValueSource(strings = {"users_email_key", "\"users_email_key\"", "public.users_email_key"})
    void shouldIdentifyEmailConflictFromPostgresConstraintName(String constraintName) {
        final RuntimeException translated = RegularUserRepositoryAdapter.translateConflict(
                new PersistenceException("Flush failed", postgresUnique(constraintName)));

        assertInstanceOf(DuplicateRegistrationException.class, translated);
        assertEquals("email already registered", translated.getMessage());
    }

    @Test
    void shouldIdentifyEmailConflictWhenTheUniqueViolationIsChainedAfterAnAbortedTransaction() {
        final SQLException aborted = new SQLException("current transaction is aborted", "25P02");
        aborted.setNextException(postgresUnique("users_email_key"));

        final RuntimeException translated = RegularUserRepositoryAdapter.translateConflict(
                new TransactionSystemException("Could not commit JPA transaction", aborted));

        assertInstanceOf(DuplicateRegistrationException.class, translated);
        assertEquals("email already registered", translated.getMessage());
    }

    private static DataIntegrityViolationException violation(String sqlState, String constraintName, String message) {
        final SQLException sqlException = new SQLException(message, sqlState);
        final ConstraintViolationException violation = new ConstraintViolationException(
                message, sqlException, constraintName);
        return new DataIntegrityViolationException("Save failed", violation);
    }

    private static PSQLException postgresUnique(String constraintName) {
        return new PSQLException(new ServerErrorMessage("C23505\0n" + constraintName + "\0Mduplicate key\0"));
    }
}
