package com.escapa.backend.adapters.dto.certificate;

import java.time.LocalDate;

public record CertificateFlatProjectionDTO(
    LocalDate conclusionDate,
    Integer workload,
    String verificationCode,

    String studentName,
    String studentAvatarUrl,
    boolean studentIsVerified,

    String courseId,
    String courseTitle,
    String courseDescription,
    String courseCategory,
    String courseLevel,
    String courseImageUrl,
    Integer courseDurationTime,
    Integer courseLessonsCount,
    Double courseRating,
    Integer courseReviewsCount,
    String courseInstructorName,
    Double coursePrice
) {}