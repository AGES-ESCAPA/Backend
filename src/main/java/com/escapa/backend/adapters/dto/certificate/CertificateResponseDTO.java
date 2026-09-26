package com.escapa.backend.adapters.dto.certificate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CertificateResponseDTO {
    private final CertificateSummaryDTO certificate;
    private final StudentSummaryDTO student;
    private final CourseSummaryDTO course;
    private final boolean owner;

    public CertificateResponseDTO(
            CertificateSummaryDTO certificate,
            StudentSummaryDTO student,
            CourseSummaryDTO course,
            boolean owner
    ) {
        this.certificate = certificate;
        this.student = student;
        this.course = course;
        this.owner = owner;
    }

    public CertificateSummaryDTO getCertificate() {
        return certificate;
    }

    public StudentSummaryDTO getStudent() {
        return student;
    }

    public CourseSummaryDTO getCourse() {
        return course;
    }

@JsonProperty ("isOwner")
    public boolean isOwner() {
        return owner;
    }
}
