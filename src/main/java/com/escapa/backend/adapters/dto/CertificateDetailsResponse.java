package com.escapa.backend.adapters.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateView;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tela de certificado digital (US-18). {@code certificate}, {@code student} e
 * {@code course} ficam separados para o front passar {@code course} direto ao
 * CourseCard, sem remapear campos.
 */
public record CertificateDetailsResponse(
        CertificateResponse certificate,
        StudentResponse student,
        CourseResponse course,
        @JsonProperty("isOwner") boolean isOwner
) {
    public record CertificateResponse(
            LocalDate conclusionDate,
            /** Carga horaria em minutos (mesmo valor de course.durationTime). */
            Integer workload,
            String verificationCode
    ) {
    }

    public record StudentResponse(
            String name,
            String avatarUrl,
            @JsonProperty("isVerified") boolean isVerified
    ) {
    }

    public record CourseResponse(
            UUID id,
            String title,
            String description,
            String category,
            String level,
            String thumbnailUrl,
            Integer durationTime,
            Integer lessonsCount,
            Double rating,
            long reviewsCount,
            String instructor,
            Double price
    ) {
    }

    public static CertificateDetailsResponse from(CertificateView view) {
        final CertificateDetails details = view.details();
        final CertificateDetails.Student student = details.student();
        final CertificateDetails.Course course = details.course();
        return new CertificateDetailsResponse(
                new CertificateResponse(
                        details.conclusionDate(),
                        details.workloadMinutes(),
                        details.verificationCode()),
                new StudentResponse(student.name(), student.avatarUrl(), student.verified()),
                new CourseResponse(
                        course.id(),
                        course.title(),
                        course.description(),
                        course.category(),
                        course.level(),
                        course.thumbnailUrl(),
                        course.durationTime(),
                        course.lessonsCount(),
                        course.rating(),
                        course.reviewsCount(),
                        course.instructor(),
                        course.price()),
                view.owner()
        );
    }
}
