package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserStatus;

class AccountInviteRepositoryAdapterTest extends PostgresIntegrationTest {

    private static final long VALIDITY_HOURS = 72;

    @Autowired
    private AccountInviteRepositoryPort accountInviteRepositoryPort;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID companyId;
    private UUID employeeId;

    @AfterEach
    void cleanUp() {
        // account_invites sai junto pelo ON DELETE CASCADE de users e company.
        if (employeeId != null) {
            jdbcTemplate.update("delete from users where id = ?", employeeId);
        }
        if (companyId != null) {
            jdbcTemplate.update("delete from users where id = ?", companyId);
        }
    }

    private void givenCompanyAndPendingEmployee() {
        companyId = UUID.randomUUID();
        jdbcTemplate.update("insert into users (id, name, email, password_hash, role) values (?, 'Empresa', ?, 'h', 'COMPANY')",
                companyId, companyId + "@empresa.com");
        jdbcTemplate.update("insert into company (id, company_name) values (?, 'Empresa')", companyId);
        final User employee = new User(null, "Colaborador", UUID.randomUUID() + "@empresa.com", "unusable",
                "STUDENT", null, UserStatus.PENDING, LocalDateTime.now());
        employeeId = userRepositoryPort.save(employee).getId();
    }

    private static String sha256(String token) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void shouldSaveAnInviteAndFindItByTokenHash() {
        givenCompanyAndPendingEmployee();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now();

        final AccountInvite saved = accountInviteRepositoryPort.save(
                AccountInvite.issue(employeeId, companyId, tokenHash, now, VALIDITY_HOURS));

        assertNotNull(saved.getId());
        final Optional<AccountInvite> found = accountInviteRepositoryPort.findByTokenHash(tokenHash);
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(employeeId, found.get().getUserId());
        assertEquals(companyId, found.get().getCompanyId());
        assertEquals(tokenHash, found.get().getTokenHash());
        assertNull(found.get().getUsedAt());
        assertFalse(found.get().isUsed());
        assertFalse(found.get().isExpired(now));
    }

    @Test
    void shouldMarkTheInviteAsUsedWhenThePasswordIsCreated() {
        givenCompanyAndPendingEmployee();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now();
        final AccountInvite saved = accountInviteRepositoryPort.save(
                AccountInvite.issue(employeeId, companyId, tokenHash, now, VALIDITY_HOURS));

        saved.markAsUsed(now.plusMinutes(5));
        accountInviteRepositoryPort.save(saved);

        final AccountInvite reloaded = accountInviteRepositoryPort.findByTokenHash(tokenHash).orElseThrow();
        assertEquals(saved.getId(), reloaded.getId());
        assertTrue(reloaded.isUsed());
        assertNotNull(reloaded.getUsedAt());
    }

    @Test
    void shouldReturnEmptyForAnUnknownTokenHash() {
        assertTrue(accountInviteRepositoryPort.findByTokenHash(sha256("inexistente")).isEmpty());
    }

    @Test
    void shouldKnowWhichUsersAlreadyHadAnInvite() {
        givenCompanyAndPendingEmployee();
        assertFalse(accountInviteRepositoryPort.existsByUserId(employeeId));

        accountInviteRepositoryPort.save(
                AccountInvite.issue(employeeId, companyId, sha256("t-" + UUID.randomUUID()), LocalDateTime.now(), VALIDITY_HOURS));

        assertTrue(accountInviteRepositoryPort.existsByUserId(employeeId));
    }

    @Test
    void shouldRejectTwoInvitesWithTheSameTokenHash() {
        givenCompanyAndPendingEmployee();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now();
        accountInviteRepositoryPort.save(AccountInvite.issue(employeeId, companyId, tokenHash, now, VALIDITY_HOURS));

        assertThrows(DataIntegrityViolationException.class, () -> accountInviteRepositoryPort.save(
                AccountInvite.issue(employeeId, companyId, tokenHash, now, VALIDITY_HOURS)));
    }

    @Test
    void shouldTreatAnInviteAsExpiredOnceTheValidityEnds() {
        final LocalDateTime now = LocalDateTime.now();
        final AccountInvite invite = AccountInvite.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", now, VALIDITY_HOURS);

        assertFalse(invite.isExpired(now.plusHours(VALIDITY_HOURS).minusSeconds(1)));
        assertTrue(invite.isExpired(now.plusHours(VALIDITY_HOURS)));
    }

    @Test
    void shouldPersistThePendingStatusOfTheUser() {
        givenCompanyAndPendingEmployee();

        assertEquals(UserStatus.PENDING, userRepositoryPort.findById(employeeId).orElseThrow().getStatus());
    }

    @Test
    void shouldCascadeDeleteInvitesWhenTheUserIsRemoved() {
        givenCompanyAndPendingEmployee();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        accountInviteRepositoryPort.save(
                AccountInvite.issue(employeeId, companyId, tokenHash, LocalDateTime.now(), VALIDITY_HOURS));

        jdbcTemplate.update("delete from users where id = ?", employeeId);

        assertTrue(accountInviteRepositoryPort.findByTokenHash(tokenHash).isEmpty());
    }
}
