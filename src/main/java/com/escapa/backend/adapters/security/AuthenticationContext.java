package com.escapa.backend.adapters.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** Le a identidade que o filtro JWT colocou no contexto do Spring Security. */
public final class AuthenticationContext {

    private AuthenticationContext() {
    }

    public static AuthenticatedUser requireUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid access token");
    }
}
