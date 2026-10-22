package com.escapa.backend.adapters.security;

import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.domain.entity.User;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Revalida no banco o ADMIN identificado pelo token da requisicao. */
@Component
public class AdminRequestGuard {

    private final AuthorizeAdminUseCase authorizeAdminUseCase;

    public AdminRequestGuard(AuthorizeAdminUseCase authorizeAdminUseCase) {
        this.authorizeAdminUseCase = authorizeAdminUseCase;
    }

    public User requireAdmin(UUID userId) {
        return authorizeAdminUseCase.execute(userId);
    }
}
