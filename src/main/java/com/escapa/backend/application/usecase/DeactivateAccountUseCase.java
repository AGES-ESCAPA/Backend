package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.port.UsersCompanyRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.CompanyEmployeeCannotDeactivateException;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.user.UserAlreadyInactiveException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;

public class DeactivateAccountUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final UsersCompanyRepositoryPort usersCompanyRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public DeactivateAccountUseCase(UserRepositoryPort userRepositoryPort,
                                     UsersCompanyRepositoryPort usersCompanyRepositoryPort,
                                     PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.usersCompanyRepositoryPort = usersCompanyRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    public void execute(UUID userId, String currentPassword) {
        final User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (usersCompanyRepositoryPort.existsByUserId(userId)) {
            throw new CompanyEmployeeCannotDeactivateException(userId);
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UserAlreadyInactiveException(userId);
        }

        if (!passwordHasherPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepositoryPort.save(user);
    }
}