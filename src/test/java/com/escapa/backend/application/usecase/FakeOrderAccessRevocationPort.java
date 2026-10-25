package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.OrderAccessRevocationPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Registra as revogações pedidas e pode ser configurado para falhar. */
public final class FakeOrderAccessRevocationPort implements OrderAccessRevocationPort {

    public record Revocation(UUID ownerId, UUID courseId, LocalDateTime revokedAt) {
    }

    private final List<Revocation> studentRevocations = new ArrayList<>();
    private final List<Revocation> companyRevocations = new ArrayList<>();
    private RuntimeException failure;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public List<Revocation> studentRevocations() {
        return studentRevocations;
    }

    public List<Revocation> companyRevocations() {
        return companyRevocations;
    }

    @Override
    public void revokeStudentCourseAccess(UUID userId, UUID courseId, LocalDateTime revokedAt) {
        failIfConfigured();
        studentRevocations.add(new Revocation(userId, courseId, revokedAt));
    }

    @Override
    public void revokeCompanyCourseAccess(UUID companyId, UUID courseId, LocalDateTime revokedAt) {
        failIfConfigured();
        companyRevocations.add(new Revocation(companyId, courseId, revokedAt));
    }

    private void failIfConfigured() {
        if (failure != null) {
            throw failure;
        }
    }
}
