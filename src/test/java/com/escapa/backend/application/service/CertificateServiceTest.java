package com.escapa.backend.application.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.escapa.backend.adapters.dto.certificate.CertificateFlatProjectionDTO;
import com.escapa.backend.adapters.dto.certificate.CertificateResponseDTO;
import com.escapa.backend.adapters.exception.ResourceNotFoundException;
import com.escapa.backend.infrastructure.persistence.CertificateRepository;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    private static final String CODE = "ESC-21AGO25-7X9L2M3N";
    private static final String COURSE_ID = "b2c3d4e5-1111-2222-3333-444455556666";

    @Mock
    private CertificateRepository certificateRepository;

    @InjectMocks
    private CertificateService certificateService;

    @Test
    void shouldReturnCertificateWhenCodeExists() {
        when(certificateRepository.findCertificateDetailsByVerificationCode(CODE))
                .thenReturn(Optional.of(new CertificateFlatProjectionDTO(
                        LocalDate.of(2026, 9, 25),
                        120,
                        CODE,
                        "Maria Silva",
                        "https://cdn.example.com/avatar.png",
                        true,
                        COURSE_ID,
                        "Curso de Turismo",
                        "Descrição do curso",
                        "Turismo",
                        "Intermediario",
                        "https://img.example.com/course.png",
                        40,
                        12,
                        4.8,
                        20,
                        "João Instrutor",
                        89.9
                )));

        final CertificateResponseDTO response = certificateService.getCertificateByCode(CODE);

        assertEquals(CODE, response.getCertificate().getVerificationCode());
        assertEquals(LocalDate.of(2026, 9, 25), response.getCertificate().getConclusionDate());
        assertEquals(120, response.getCertificate().getWorkload());

        assertEquals("Maria Silva", response.getStudent().getName());
        assertEquals("https://cdn.example.com/avatar.png", response.getStudent().getAvatarUrl());
        assertTrue(response.getStudent().isVerified());

        assertEquals(UUID.fromString(COURSE_ID), response.getCourse().getId());
        assertEquals("Curso de Turismo", response.getCourse().getTitle());
        assertEquals("Turismo", response.getCourse().getCategory());
        assertEquals("https://img.example.com/course.png", response.getCourse().getThumbnailUrl());
        assertEquals(40, response.getCourse().getDurationTime());
        assertEquals(12, response.getCourse().getLessonsCount());
        assertEquals(4.8, response.getCourse().getRating(), 0.001);
        assertEquals(20, response.getCourse().getReviewsCount());
        assertEquals("João Instrutor", response.getCourse().getInstructor());
        assertEquals(89.9, response.getCourse().getPrice(), 0.001);
    }

    @Test
    void shouldThrowWhenCodeDoesNotExist() {
        when(certificateRepository.findCertificateDetailsByVerificationCode("INEXISTENTE"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> certificateService.getCertificateByCode("INEXISTENTE"));
    }

    @Test
    void shouldThrowWhenCourseNotConcluded() {
        when(certificateRepository.findCertificateDetailsByVerificationCode(CODE))
                .thenReturn(Optional.of(new CertificateFlatProjectionDTO(
                        null, // conclusionDate nulo = curso não concluído
                        120,
                        CODE,
                        "Maria Silva",
                        "https://cdn.example.com/avatar.png",
                        true,
                        COURSE_ID,
                        "Curso de Turismo",
                        "Descrição do curso",
                        "Turismo",
                        "Intermediario",
                        "https://img.example.com/course.png",
                        40,
                        12,
                        4.8,
                        20,
                        "João Instrutor",
                        89.9
                )));

        assertThrows(ResourceNotFoundException.class,
                () -> certificateService.getCertificateByCode(CODE));
    }
}
