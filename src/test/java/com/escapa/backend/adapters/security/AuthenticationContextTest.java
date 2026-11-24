package com.escapa.backend.adapters.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

class AuthenticationContextTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoAuthenticatedUser() {
        assertTrue(AuthenticationContext.currentUser().isEmpty());
        final ResponseStatusException error = assertThrows(
                ResponseStatusException.class, AuthenticationContext::requireUser);
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("Missing or invalid access token", error.getReason());
    }

    @Test
    void shouldReturnAuthenticatedUserFromSecurityContext() {
        final UUID userId = UUID.randomUUID();
        final AuthenticatedUser user = new AuthenticatedUser(userId, "aluno@escapa.com", "STUDENT", "STUDENT");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_STUDENT")));

        assertEquals(user, AuthenticationContext.currentUser().orElseThrow());
        assertEquals(userId, AuthenticationContext.requireUser().id());
    }

    @Test
    void shouldIgnoreNonAuthenticatedUserPrincipals() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "anonymous", null, AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertTrue(AuthenticationContext.currentUser().isEmpty());
    }
}
