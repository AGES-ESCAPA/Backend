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
 * Token de recuperacao de senha. Guarda so o hash: o token em si existe apenas no
 * link enviado por e-mail.
 */
public class PasswordResetToken {
    private static final int TOKEN_BYTES = 32;

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private final LocalDateTime createdAt;

    public PasswordResetToken(
            UUID id,
            UUID userId,
            String tokenHash,
            LocalDateTime expiresAt,
            LocalDateTime usedAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static PasswordResetToken issue(UUID userId, String tokenHash, LocalDateTime now, long validityHours) {
        return new PasswordResetToken(null, userId, tokenHash, now.plusHours(validityHours), null, now);
    }

    /** Token do link: 256 bits aleatorios em base64 seguro para URL. */
    public static String generateToken() {
        final byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 do token em hex minusculo, o mesmo formato gravado em {@code token_hash}. */
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
