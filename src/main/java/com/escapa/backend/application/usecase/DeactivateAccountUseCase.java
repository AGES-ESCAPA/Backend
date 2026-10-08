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

public class DeactivateAccountUseCase {

    private static final String INACTIVE_STATUS = "INACTIVE";

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

        if (INACTIVE_STATUS.equals(user.getStatus())) {
            throw new UserAlreadyInactiveException(userId);
        }

        if (!passwordHasherPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setStatus(INACTIVE_STATUS);
        userRepositoryPort.save(user);
    }
}