package com.escapa.backend.application.usecase;

import java.util.List;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

public class ListUsersUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public ListUsersUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public List<User> execute() {
        return userRepositoryPort.findAll();
    }
}
