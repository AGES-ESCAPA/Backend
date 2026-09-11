package com.escapa.backend.user.entity;

/**
 * Papel do usuário. Gravado como texto em {@code users.role} e gêmeo do vocabulário do seed.
 * Cada papel corresponde a uma subclasse JPA da herança JOINED:
 * {@code STUDENT} → {@link RegularUserEntity}, {@code ADMIN} → {@link AdminEntity},
 * {@code COMPANY} → {@link CompanyEntity}.
 */
public enum UserRole {
    STUDENT,
    ADMIN,
    COMPANY
}
