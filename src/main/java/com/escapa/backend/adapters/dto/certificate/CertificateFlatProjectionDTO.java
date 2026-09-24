package com.escapa.backend.adapters.dto.certificate;

public record CertificateFlatProjectionDTO(
    String conclusionDate,
    Integer workload,
    String verificationCode,

    String studentName,
    String studentAvatarUrl,
    boolean studentIsVerified,

    String courseId,
    String courseTitle,
    String courseDescription,
    String courseCategory,
    Integer courseLevel,
    String courseImageUrl,
    Integer courseDurationTime,
    Integer courseLessonsCount,
    double courseRating,
    Integer courseReviewsCount,
    String courseInstructorName,
    double coursePrice
) {}