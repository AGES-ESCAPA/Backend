package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListUsersUseCaseTest {

    @Test
    void shouldListAllUsers() {
        UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        repository.save(User.create("Maria", "maria@email.com", "student"));
        repository.save(User.create("Joao", "joao@email.com", "teacher"));
        ListUsersUseCase useCase = new ListUsersUseCase(repository);

        List<User> users = useCase.execute();

        assertEquals(2, users.size());
    }
}
