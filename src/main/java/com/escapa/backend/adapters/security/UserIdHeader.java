package com.escapa.backend.adapters.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Leitura do header provisorio {@code X-User-Id}, que identifica o usuario ate o login (US-23)
 * trazer o token. Centraliza a validacao antes repetida em cada controller.
 */
public final class UserIdHeader {

    public static final String NAME = "X-User-Id";

    private UserIdHeader() {
    }

    /** Rotas do aluno: header ausente ou invalido resulta em 401. */
    public static UUID requireStudentId(String headerValue) {
        return parse(headerValue == null ? null : headerValue.trim(), HttpStatus.UNAUTHORIZED);
    }

    /** Rotas admin: header ausente ou invalido resulta em 403. */
    static UUID requireAdminId(String headerValue) {
        return parse(headerValue, HttpStatus.FORBIDDEN);
    }

    private static UUID parse(String headerValue, HttpStatus failureStatus) {
        if (headerValue == null || headerValue.isBlank()) {
            throw new ResponseStatusException(failureStatus, "Missing " + NAME + " header");
        }
        try {
            return UUID.fromString(headerValue);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(failureStatus, "Invalid " + NAME + ": must be a valid UUID");
        }
    }
}
