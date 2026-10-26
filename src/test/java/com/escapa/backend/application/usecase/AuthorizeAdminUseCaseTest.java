package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AdminAccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizeAdminUseCaseTest {

    private final UserRepositoryPort repository = new InMemoryUserRepositoryPort();
    private final AuthorizeAdminUseCase useCase = new AuthorizeAdminUseCase(repository);

    @Test
    void shouldReturnTheUserWhenItIsAnAdmin() {
        final User admin = repository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));

        final User result = useCase.execute(admin.getId());

        assertEquals(admin.getId(), result.getId());
    }

    @Test
    void shouldAcceptTheAdminTypeInAnyCase() {
        final User admin = repository.save(new User("Admin Um", "admin@escapa.com", "hash", "admin"));

        assertEquals(admin.getId(), useCase.execute(admin.getId()).getId());
    }

    @Test
    void shouldRejectUnknownUser() {
        final AdminAccessDeniedException exception = assertThrows(
                AdminAccessDeniedException.class,
                () -> useCase.execute(UUID.randomUUID())
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void shouldRejectNonAdminUser() {
        final User student = repository.save(new User("Aluno Um", "aluno@escapa.com", "hash", "STUDENT"));

        final AdminAccessDeniedException exception = assertThrows(
                AdminAccessDeniedException.class,
                () -> useCase.execute(student.getId())
        );

        assertEquals("Access denied: user is not ADMIN", exception.getMessage());
    }
}
