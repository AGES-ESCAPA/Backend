package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.CannotDeactivateOwnAccountException;
import com.escapa.backend.domain.user.UserAlreadyInactiveException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;

/**
 * Bloqueio administrativo: marca a conta como {@code INACTIVE} e invalida convites abertos.
 * Diferente da autodesativação, não exige senha e vale também para colaboradores.
 */
public class AdminDeactivateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final AccountInviteRepositoryPort accountInviteRepositoryPort;
    private final Clock clock;

    public AdminDeactivateUserUseCase(
            UserRepositoryPort userRepositoryPort,
            AccountInviteRepositoryPort accountInviteRepositoryPort,
            Clock clock) {
        this.userRepositoryPort = userRepositoryPort;
        this.accountInviteRepositoryPort = accountInviteRepositoryPort;
        this.clock = clock;
    }

    public User execute(UUID actorId, UUID targetId) {
        if (actorId != null && actorId.equals(targetId)) {
            throw new CannotDeactivateOwnAccountException();
        }

        final User user = userRepositoryPort.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UserAlreadyInactiveException(targetId);
        }

        user.setStatus(UserStatus.INACTIVE);
        final User saved = userRepositoryPort.save(user);
        accountInviteRepositoryPort.invalidateOpenInvites(targetId, LocalDateTime.now(clock));
        return saved;
    }
}
