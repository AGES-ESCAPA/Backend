package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

public class GetUserByIdUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public GetUserByIdUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public User execute(String id) {
        return userRepositoryPort.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
