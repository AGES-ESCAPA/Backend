package com.escapa.backend.application.model;

import java.util.UUID;

/** Identidade assinada no JWT e usada apenas para autenticar a requisicao. */
public record TokenClaims(UUID userId, String email, String role, String profile) {
}
