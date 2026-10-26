package com.escapa.backend.application.model;

/**
 * Certificado como visto por quem acessa o link publico (US-18).
 *
 * {@code owner} indica se o visitante autenticado e o dono do certificado,
 * pela mesma regra que libera o download ({@link CertificateDetails#isOwnedBy}).
 * Visitante anonimo recebe sempre {@code false}.
 */
public record CertificateView(CertificateDetails details, boolean owner) {
}
