package com.escapa.backend.application.service;

import org.springframework.stereotype.Service;

import com.escapa.backend.adapters.dto.certificate.CertificateFlatProjectionDTO;
import com.escapa.backend.adapters.dto.certificate.CertificateResponseDTO;
import com.escapa.backend.adapters.dto.certificate.CertificateSummaryDTO;
import com.escapa.backend.adapters.dto.certificate.CourseSummaryDTO;
import com.escapa.backend.adapters.dto.certificate.StudentSummaryDTO;
import com.escapa.backend.adapters.exception.ResourceNotFoundException;
import com.escapa.backend.infrastructure.persistence.CertificateRepository;

@Service
public class CertificateService {
    private final CertificateRepository certificateRepository;

    public CertificateService(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    public CertificateResponseDTO getCertificateByCode(String verificationCode) {
        final CertificateFlatProjectionDTO flatDto = loadCertificate(verificationCode);
        validateCertificate(flatDto);
        return buildResponse(flatDto);
    }

    private CertificateFlatProjectionDTO loadCertificate(String verificationCode) {
        return certificateRepository
                .findCertificateDetailsByVerificationCode(verificationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found."));
    }

    private void validateCertificate(CertificateFlatProjectionDTO flatDto) {
        if (flatDto.conclusionDate() == null) {
            throw new ResourceNotFoundException("Certificate not found.");
        }
    }

    private CertificateResponseDTO buildResponse(CertificateFlatProjectionDTO flatDto) {
        final CertificateSummaryDTO certificateDto = new CertificateSummaryDTO (
            flatDto.conclusionDate(),
            flatDto.verificationCode(),
            flatDto.workload()
        );
        final StudentSummaryDTO studentDto = new StudentSummaryDTO(
                flatDto.studentName(),
                flatDto.studentAvatarUrl(),
                flatDto.studentIsVerified()
        );

        final CourseSummaryDTO courseDto = new CourseSummaryDTO(
                flatDto.courseId(),
                flatDto.courseTitle(),
                flatDto.courseDescription(),
                flatDto.courseCategory(),
                flatDto.courseLevel(),
                flatDto.courseImageUrl(),
                flatDto.courseDurationTime(),
                flatDto.courseLessonsCount(),
                flatDto.courseRating() == null ? 0.0 : flatDto.courseRating(),
                flatDto.courseReviewsCount(),
                flatDto.courseInstructorName(),
                flatDto.coursePrice() == null ? 0.0 : flatDto.coursePrice()
        );

        return new CertificateResponseDTO(certificateDto, studentDto, courseDto, false);
    }
}