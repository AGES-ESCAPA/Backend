package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Garante, direto no banco, a restrição ck_users_status da migration V16. */
class UsersStatusSchemaTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
    }

    private void insertWithStatus(String status) {
        userId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, role, status) values (?, 'U', ?, 'h', 'STUDENT', ?)",
                userId, userId + "@email.com", status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "PENDING", "INACTIVE"})
    void shouldAcceptEveryUserStatus(String status) {
        assertDoesNotThrow(() -> insertWithStatus(status));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLOCKED", "pending", ""})
    void shouldRejectAnUnknownUserStatus(String status) {
        assertThrows(DataIntegrityViolationException.class, () -> insertWithStatus(status));
    }
}
