package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import com.escapa.backend.application.port.ContactValidatorPort;
import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.UserNotFoundException;

/**
 * Admin solicita troca de e-mail: o endereço ativo não muda até a confirmação
 * no link enviado ao endereço novo.
 */
public class RequestAdminEmailChangeUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort;
    private final ContactValidatorPort contactValidatorPort;
    private final Clock clock;
    private final long expirationHours;

    public RequestAdminEmailChangeUseCase(
            UserRepositoryPort userRepositoryPort,
            EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort,
            ContactValidatorPort contactValidatorPort,
            Clock clock,
            long expirationHours
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.emailChangeTokenRepositoryPort = emailChangeTokenRepositoryPort;
        this.contactValidatorPort = contactValidatorPort;
        this.clock = clock;
        this.expirationHours = expirationHours;
    }

    public EmailChangeMailCommand execute(UUID actorId, UUID targetUserId, String newEmail) {
        final String normalized = requireValidEmail(newEmail);
        final User user = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (normalized.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("email is the same as the current one");
        }

        userRepositoryPort.findByEmail(normalized)
                .filter(owner -> !owner.getId().equals(targetUserId))
                .ifPresent(owner -> {
                    throw new DuplicateRegistrationException("email");
                });

        final LocalDateTime now = LocalDateTime.now(clock);
        emailChangeTokenRepositoryPort.invalidateOpenTokens(targetUserId, now);

        final String rawToken = EmailChangeToken.generateToken();
        emailChangeTokenRepositoryPort.save(EmailChangeToken.issue(
                targetUserId,
                normalized,
                EmailChangeToken.hashToken(rawToken),
                actorId,
                now,
                expirationHours));

        return new EmailChangeMailCommand(user.getName(), normalized, rawToken);
    }

    private String requireValidEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        final String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!contactValidatorPort.isValidEmail(normalized)) {
            throw new IllegalArgumentException("email is invalid");
        }
        return normalized;
    }
}
