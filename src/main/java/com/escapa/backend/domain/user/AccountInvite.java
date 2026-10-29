package com.escapa.backend.domain.user;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite para o colaborador criar a senha. Guarda só o hash do token: o token em si
 * existe apenas no link enviado por e-mail.
 */
public class AccountInvite {
    private final UUID id;
    private final UUID userId;
    private final UUID companyId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private final LocalDateTime createdAt;

    public AccountInvite(UUID id, UUID userId, UUID companyId, String tokenHash,
                         LocalDateTime expiresAt, LocalDateTime usedAt, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.companyId = companyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static AccountInvite issue(UUID userId, UUID companyId, String tokenHash,
                                      LocalDateTime now, long validityHours) {
        return new AccountInvite(null, userId, companyId, tokenHash, now.plusHours(validityHours), null, now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void markAsUsed(LocalDateTime now) {
        this.usedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
