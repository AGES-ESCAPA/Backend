package com.escapa.backend.domain.user;

import java.util.Objects;
import java.util.UUID;

public class User {
    private final String id;
    private final String name;
    private final String email;
    private final String role;

    public User(String id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public static User create(String name, String email, String role) {
        return new User(UUID.randomUUID().toString(), normalizeName(name), normalizeEmail(email), normalizeRole(role));
    }

    private static String normalizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Name is required");
        }
        final String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        return normalized;
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Email is required");
        }
        final String normalized = value.trim();
        if (normalized.isBlank() || !normalized.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        return normalized.toLowerCase();
    }

    private static String normalizeRole(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role is required");
        }
        return value.trim().toUpperCase();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final User user = (User) o;
        return Objects.equals(id, user.id)
                && Objects.equals(name, user.name)
                && Objects.equals(email, user.email)
                && Objects.equals(role, user.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, role);
    }
}
