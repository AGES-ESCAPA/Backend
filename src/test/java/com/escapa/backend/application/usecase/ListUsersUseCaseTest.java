package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;

class ListUsersUseCaseTest {

    @Test
    void shouldListAllUsers() {
        final InMemoryUserRepositoryPort repository = new InMemoryUserRepositoryPort();
        repository.save(new User("Maria Silva", "maria@email.com", "hash1", "STUDENT"));
        repository.save(new User("Joao Souza", "joao@email.com", "hash2", "TEACHER"));
        final ListUsersUseCase useCase = new ListUsersUseCase(repository);

        final List<User> users = useCase.execute();

        assertEquals(2, users.size());
    }

    @Test
    void shouldExposeACompanyLinkedStudentAsEmployee() {
        final InMemoryUserRepositoryPort repository = new InMemoryUserRepositoryPort();
        final User linked = repository.save(new User("Luciana", "luciana@email.com", "hash", "STUDENT"));
        final User free = repository.save(new User("Ana", "ana@email.com", "hash", "STUDENT"));
        repository.linkUserToCompany(linked.getId());
        final ListUsersUseCase useCase = new ListUsersUseCase(repository);

        assertEquals("EMPLOYEE", useCase.resolveProfile(linked));
        assertEquals("STUDENT", useCase.resolveProfile(free));
        assertEquals("ADMIN", useCase.resolveProfile(new User("Admin", "a@e.com", "h", "ADMIN")));
    }
}
