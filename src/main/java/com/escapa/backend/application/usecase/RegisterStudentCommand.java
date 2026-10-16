package com.escapa.backend.application.usecase;

public record RegisterStudentCommand(
        String name,
        String cpf,
        String email,
        String phone,
        String password
) {
}
