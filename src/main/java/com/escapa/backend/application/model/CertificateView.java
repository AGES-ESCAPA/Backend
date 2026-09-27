package com.escapa.backend.application.model;

/**
 * Certificado como visto por quem acessa o link publico (US-18).
 *
 * {@code owner} indica se o visitante e o dono do certificado. Enquanto a
 * autenticacao nao existe, e sempre {@code false}; o campo ja faz parte do
 * contrato para nao quebrar o front quando a validacao real chegar.
 */
public record CertificateView(CertificateDetails details, boolean owner) {
}
