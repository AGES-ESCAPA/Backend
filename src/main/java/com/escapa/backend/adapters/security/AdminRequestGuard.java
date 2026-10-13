package com.escapa.backend.adapters.security;

import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.domain.entity.User;
import org.springframework.stereotype.Component;

/** Resolve o ADMIN autenticado a partir do header {@code X-User-Id} das rotas /admin. */
@Component
public class AdminRequestGuard {

    private final AuthorizeAdminUseCase authorizeAdminUseCase;

    public AdminRequestGuard(AuthorizeAdminUseCase authorizeAdminUseCase) {
        this.authorizeAdminUseCase = authorizeAdminUseCase;
    }

    public User requireAdmin(String userIdHeader) {
        return authorizeAdminUseCase.execute(UserIdHeader.requireAdminId(userIdHeader));
    }
}
