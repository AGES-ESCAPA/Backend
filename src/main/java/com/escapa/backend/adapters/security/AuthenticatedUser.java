package com.escapa.backend.adapters.security;

import java.util.UUID;

/** Usuario identificado por um JWT valido durante uma requisicao. */
public record AuthenticatedUser(UUID id, String email, String role, String profile) {
}
