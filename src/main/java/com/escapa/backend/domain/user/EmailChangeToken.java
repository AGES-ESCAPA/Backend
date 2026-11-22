package com.escapa.backend.domain.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Token de confirmação de troca de e-mail. Guarda só o hash: o token em si existe
 * apenas no link enviado ao endereço novo. Distinto de {@link PasswordResetToken}.
 */
public class EmailChangeToken {
    private static final int TOKEN_BYTES = 32;

    private final UUID id;
    private final UUID userId;
    private final String newEmail;
    private final String tokenHash;
    private final UUID requestedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private final LocalDateTime createdAt;

    public EmailChangeToken(
            UUID id,
            UUID userId,
            String newEmail,
            String tokenHash,
            UUID requestedBy,
            LocalDateTime expiresAt,
            LocalDateTime usedAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.newEmail = newEmail;
        this.tokenHash = tokenHash;
        this.requestedBy = requestedBy;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static EmailChangeToken issue(
            UUID userId,
            String newEmail,
            String tokenHash,
            UUID requestedBy,
            LocalDateTime now,
            long validityHours
    ) {
        return new EmailChangeToken(
                null, userId, newEmail, tokenHash, requestedBy, now.plusHours(validityHours), null, now);
    }

    public static String generateToken() {
        final byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String token) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
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

    public String getNewEmail() {
        return newEmail;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getRequestedBy() {
        return requestedBy;
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
