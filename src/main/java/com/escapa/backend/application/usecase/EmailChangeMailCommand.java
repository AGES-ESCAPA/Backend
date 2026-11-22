package com.escapa.backend.application.usecase;

public record EmailChangeMailCommand(String name, String newEmail, String token) {
}
