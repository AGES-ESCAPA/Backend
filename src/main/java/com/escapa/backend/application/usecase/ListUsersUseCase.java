package com.escapa.backend.application.usecase;

import java.util.List;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserTypes;

public class ListUsersUseCase {
    private static final String EMPLOYEE = "EMPLOYEE";

    private final UserRepositoryPort userRepositoryPort;

    public ListUsersUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public List<User> execute() {
        return userRepositoryPort.findAll();
    }

    /**
     * Perfil exibido na gestão de usuários: o mesmo critério do login.
     * STUDENT com vínculo em {@code users_company} vira EMPLOYEE.
     */
    public String resolveProfile(User user) {
        if (user == null) {
            return null;
        }
        if (UserTypes.STUDENT.equals(user.getUserType())
                && user.getId() != null
                && userRepositoryPort.isUserLinkedToCompany(user.getId())) {
            return EMPLOYEE;
        }
        return user.getUserType();
    }
}
