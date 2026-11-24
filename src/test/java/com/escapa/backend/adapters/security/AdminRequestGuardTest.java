package com.escapa.backend.adapters.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AdminAccessDeniedException;
import com.escapa.backend.domain.user.UserTypes;

class AdminRequestGuardTest {

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final AdminRequestGuard guard = new AdminRequestGuard(new AuthorizeAdminUseCase(users));

    @Test
    void shouldReturnAdminUserWhenAuthorized() {
        final User admin = users.save(new User("Barbara", "barbara@escapa.com", "hash", UserTypes.ADMIN));

        final User authorized = guard.requireAdmin(admin.getId());

        assertEquals(admin.getId(), authorized.getId());
        assertEquals(UserTypes.ADMIN, authorized.getUserType());
    }

    @Test
    void shouldRejectNonAdminUser() {
        final User student = users.save(new User("Mariana", "mariana@email.com", "hash", UserTypes.STUDENT));

        assertThrows(AdminAccessDeniedException.class, () -> guard.requireAdmin(student.getId()));
    }
}
