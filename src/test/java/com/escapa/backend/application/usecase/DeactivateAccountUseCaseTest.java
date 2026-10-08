package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.CompanyEmployeeCannotDeactivateException;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.user.UserAlreadyInactiveException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeactivateAccountUseCaseTest {

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryUsersCompanyRepositoryPort usersCompanyRepository = new InMemoryUsersCompanyRepositoryPort();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final DeactivateAccountUseCase useCase =
            new DeactivateAccountUseCase(userRepository, usersCompanyRepository, hasher);

    private User createUser(String rawPassword) {
        final User user = new User("Maria Silva", "maria@email.com", hasher.hash(rawPassword), "STUDENT");
        return userRepository.save(user);
    }

    @Test
    void shouldDeactivateAccountWhenPasswordIsCorrect() {
        final User user = createUser("password123");

        useCase.execute(user.getId(), "password123");

        final User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("INACTIVE", updated.getStatus());
    }

    @Test
    void shouldRejectWrongCurrentPassword() {
        final User user = createUser("password123");

        assertThrows(
                InvalidCurrentPasswordException.class,
                () -> useCase.execute(user.getId(), "wrong-password")
        );
    }

    @Test
    void shouldRejectCompanyEmployeeSelfDeactivation() {
        final User user = createUser("password123");
        usersCompanyRepository.linkToCompany(user.getId());

        assertThrows(
                CompanyEmployeeCannotDeactivateException.class,
                () -> useCase.execute(user.getId(), "password123")
        );
    }

    @Test
    void shouldRejectAlreadyInactiveAccount() {
        final User user = createUser("password123");
        user.setStatus("INACTIVE");
        userRepository.save(user);

        assertThrows(
                UserAlreadyInactiveException.class,
                () -> useCase.execute(user.getId(), "password123")
        );
    }
}