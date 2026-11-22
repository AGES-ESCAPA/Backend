package com.escapa.backend.application.usecase;

/** Dados para o controller enviar o e-mail depois do commit do pedido de recuperacao. */
public record PasswordResetMailCommand(String name, String email, String token) {
}
