package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class RequestPasswordResetUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryPasswordResetTokenRepositoryPort tokenRepository =
            new InMemoryPasswordResetTokenRepositoryPort();
    private final RequestPasswordResetUseCase useCase =
            new RequestPasswordResetUseCase(userRepository, tokenRepository, CLOCK, 2);

    @Test
    void shouldIssueTokenForActiveUser() {
        final User user = userRepository.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));

        final Optional<PasswordResetMailCommand> result = useCase.execute("Ana@escapa.com");

        assertTrue(result.isPresent());
        assertEquals("Ana", result.get().name());
        assertEquals("ana@escapa.com", result.get().email());
        assertTrue(tokenRepository.findByTokenHash(
                PasswordResetToken.hashToken(result.get().token())).isPresent());
        assertEquals(user.getId(), tokenRepository.findByTokenHash(
                PasswordResetToken.hashToken(result.get().token())).orElseThrow().getUserId());
    }

    @Test
    void shouldReturnEmptyForUnknownEmail() {
        assertTrue(useCase.execute("nobody@escapa.com").isEmpty());
    }

    @Test
    void shouldReturnEmptyForInactiveUser() {
        userRepository.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.INACTIVE, LocalDateTime.now(CLOCK)));

        assertTrue(useCase.execute("ana@escapa.com").isEmpty());
    }

    private static final class InMemoryPasswordResetTokenRepositoryPort
            implements PasswordResetTokenRepositoryPort {

        private final Map<UUID, PasswordResetToken> byId = new HashMap<>();
        private final Map<String, UUID> idByHash = new HashMap<>();

        @Override
        public PasswordResetToken save(PasswordResetToken token) {
            final UUID id = token.getId() != null ? token.getId() : UUID.randomUUID();
            final PasswordResetToken stored = new PasswordResetToken(
                    id, token.getUserId(), token.getTokenHash(), token.getExpiresAt(),
                    token.getUsedAt(), token.getCreatedAt());
            byId.put(id, stored);
            idByHash.put(stored.getTokenHash(), id);
            return stored;
        }

        @Override
        public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
            final UUID id = idByHash.get(tokenHash);
            return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
        }

        @Override
        public boolean markAsUsed(UUID tokenId, LocalDateTime usedAt) {
            final PasswordResetToken current = byId.get(tokenId);
            if (current == null || current.isUsed()) {
                return false;
            }
            current.markAsUsed(usedAt);
            return true;
        }

        @Override
        public void invalidateOpenTokens(UUID userId, LocalDateTime now) {
            byId.values().stream()
                    .filter(token -> token.getUserId().equals(userId) && !token.isUsed())
                    .forEach(token -> token.markAsUsed(now));
        }
    }
}
