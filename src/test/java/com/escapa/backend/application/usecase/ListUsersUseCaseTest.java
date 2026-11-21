package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

class ListUsersUseCaseTest {

    @Test
    void shouldListAllUsers() {
        final UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        repository.save(new User("Maria Silva", "maria@email.com", "hash1", "STUDENT"));
        repository.save(new User("Joao Souza", "joao@email.com", "hash2", "TEACHER"));
        final ListUsersUseCase useCase = new ListUsersUseCase(repository);

        final List<User> users = useCase.execute();

        assertEquals(2, users.size());
    }
}
