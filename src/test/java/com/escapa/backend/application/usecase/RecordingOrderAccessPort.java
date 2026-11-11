package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.port.OrderAccessPort;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Registra as matrículas gravadas por userId/courseId; o resto da porta não é usado fora de pedidos. */
public final class RecordingOrderAccessPort implements OrderAccessPort {

    private final Map<String, Enrollment> enrollments = new LinkedHashMap<>();

    Enrollment enrollment(UUID userId, UUID courseId) {
        return enrollments.get(userId + ":" + courseId);
    }

    int enrollmentCount() {
        return enrollments.size();
    }

    @Override
    public void saveStudentEnrollment(UUID userId, UUID courseId, LocalDate startDate, LocalDate expirationDate) {
        enrollments.put(userId + ":" + courseId, new Enrollment(startDate, expirationDate));
    }

    @Override
    public Optional<Enrollment> findStudentEnrollment(UUID userId, UUID courseId) {
        return Optional.ofNullable(enrollment(userId, courseId));
    }

    @Override
    public Optional<Enrollment> findCompanyCourse(UUID companyId, UUID courseId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveCompanyCourse(UUID companyId, UUID courseId, LocalDate startDate, LocalDate expirationDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCompanySeats(UUID companyId, int quantity) {
        throw new UnsupportedOperationException();
    }
}
